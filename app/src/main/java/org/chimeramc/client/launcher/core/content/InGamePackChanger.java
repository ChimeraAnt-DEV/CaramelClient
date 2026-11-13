package org.chimeramc.client.core.content;

import org.chimeramc.client.util.AppLog;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Enables or disables the resource packs of an instance without leaving the game.
 *
 * <p>Bedrock tracks active packs in <b>two</b> places, and which one matters depends on when it is
 * read:
 * <ul>
 *   <li>{@code minecraftpe/global_resource_packs.json} — the list applied to a world when it is
 *       <em>loaded</em>. An array of {@code {pack_id, version}} entries.</li>
 *   <li>{@code minecraftWorlds/<world>/world_resource_packs.json} — the list the <em>running</em>
 *       world actually reads, both at load and when it refreshes its pack selection in-game.</li>
 * </ul>
 *
 * <p>Writing only the global file is why toggling a pack in-game appeared to do nothing: the
 * loaded world never re-reads the global list, so enable/disable had no visible effect until the
 * player left and re-entered. {@link #setActive} therefore writes <em>both</em> — the global list
 * for the next load, and every world's own file so the current session sees the change.
 *
 * <p>The pack folders live under {@code resource_packs/<uuid>/}.
 *
 * <p>Deliberately File-based rather than Context-based so the list/merge logic is unit-testable
 * on the JVM, the same split the rest of the content package uses.
 *
 * <p>This is the launcher's own writer for the global pack list. {@link SkinPackActivator} owns
 * the skin-pack path and records what *it* applied so it never removes a player's own choice;
 * here the toggle <em>is</em> the player's explicit instruction, so entries are added and removed
 * directly, and unrelated entries are preserved verbatim.
 */
public final class InGamePackChanger {

    private static final String RESOURCE_PACKS_DIR = "resource_packs";
    private static final String MINECRAFT_PE_DIR = "minecraftpe";
    private static final String GLOBAL_RESOURCE_PACKS = "global_resource_packs.json";
    private static final String MINECRAFT_WORLDS_DIR = "minecraftWorlds";
    private static final String WORLD_RESOURCE_PACKS = "world_resource_packs.json";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * A hook the running session can install so a pack change is pushed to the game immediately.
     *
     * <p>Writing the files is necessary but not sufficient: a loaded world caches its pack stack,
     * so without a nudge the change is only visible on the next load. A session installs a
     * {@link Reloader} that asks the game to refresh its applied packs; when none is installed the
     * write still happens and the caller is told a restart is needed.
     */
    public interface Reloader {
        /**
         * @return true when the game accepted the refresh, false when it cannot be done live (the
         *         change will apply on the next world load).
         */
        boolean reloadPacks();
    }

    private static volatile Reloader reloader;

    public static void setReloader(Reloader value) {
        reloader = value;
    }

    /**
     * A hook a running session installs so a pack change can be applied by relaunching the game.
     *
     * <p>Some builds cannot refresh their pack stack in place — the preloader exposes no
     * implementation of the reload call yet, so {@link #requestReload()} returns false. Without a
     * restart the player would have to leave the world and launch it again by hand, which is the
     * exact friction the in-game changer exists to remove. This hook lets the session relaunch
     * the same instance for the player; the write has already happened by the time it is called.
     */
    public interface Restarter {
        /** Relaunches the current instance. Returns true when a relaunch was started. */
        boolean restart();
    }

    private static volatile Restarter restarter;

    public static void setRestarter(Restarter value) {
        restarter = value;
    }

    private InGamePackChanger() {
    }

    /** One installed resource pack and whether the game currently has it active. */
    public static final class PackEntry {
        public final String uuid;
        public final String name;
        public final String version;
        public final File directory;
        public final boolean active;

        PackEntry(String uuid, String name, String version, File directory, boolean active) {
            this.uuid = uuid;
            this.name = name;
            this.version = version;
            this.directory = directory;
            this.active = active;
        }
    }

    /**
     * Lists the instance's resource packs, newest-listed first, sorted by name.
     *
     * <p>Never throws: a game data root that does not exist yet simply yields an empty list, so
     * the in-game panel shows "no packs" rather than crashing the menu.
     */
    public static List<PackEntry> listPacks(File gameDataDir) {
        List<PackEntry> result = new ArrayList<>();
        if (gameDataDir == null) return result;

        Set<String> active = activeUuids(gameDataDir);
        File resourcePacks = new File(gameDataDir, RESOURCE_PACKS_DIR);
        File[] children = resourcePacks.listFiles();
        if (children == null) return result;

        for (File child : children) {
            SkinPackActivator.PackIdentity identity = SkinPackActivator.readIdentity(child);
            if (identity == null) continue;
            String name = readPackName(child);
            result.add(new PackEntry(identity.uuid, name, identity.version, child,
                    active.contains(identity.uuid)));
        }
        result.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
        return result;
    }

    /**
     * The union of the packs found under several candidate roots, de-duplicated by uuid.
     *
     * <p>The game picks its storage from isolation and internal/external, so a pack may sit in any
     * one of the candidates. Listing only the first root showed an empty or stale list whenever
     * that root was not the one actually in use.
     */
    public static List<PackEntry> listPacks(List<File> gameDataDirs) {
        java.util.LinkedHashMap<String, PackEntry> byUuid = new java.util.LinkedHashMap<>();
        if (gameDataDirs == null) return new ArrayList<>();
        for (File dir : gameDataDirs) {
            for (PackEntry entry : listPacks(dir)) {
                // A pack active in any root counts as active.
                PackEntry existing = byUuid.get(entry.uuid);
                if (existing == null || (!existing.active && entry.active)) {
                    byUuid.put(entry.uuid, entry);
                }
            }
        }
        List<PackEntry> result = new ArrayList<>(byUuid.values());
        result.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
        return result;
    }

    /**
     * Uuids currently active for this instance.
     *
     * <p>The union of the global list and every world's own list: a pack can be active in the
     * running world without being in the global list (that is exactly the state the in-game toggle
     * produces), so reading only the global file would show "off" for a pack the player just
     * turned on.
     */
    public static Set<String> activeUuids(File gameDataDir) {
        Set<String> result = new LinkedHashSet<>();
        collectUuids(globalFile(gameDataDir), result);
        for (File worldPacks : worldPackFiles(gameDataDir)) {
            collectUuids(worldPacks, result);
        }
        return result;
    }

    private static void collectUuids(File file, Set<String> into) {
        JsonArray array = readGlobalArray(file);
        if (array == null) return;
        for (JsonElement element : array) {
            if (!element.isJsonObject()) continue;
            JsonObject object = element.getAsJsonObject();
            if (object.has("pack_id")) {
                into.add(object.get("pack_id").getAsString().trim().toLowerCase(Locale.ROOT));
            }
        }
    }

    /**
     * Turns a pack on or off for the loaded world and the next load alike.
     *
     * <p>Writes the global list (applies at next world load) and every world's own
     * {@code world_resource_packs.json} (what the running world reads). Writing only the global
     * file was the bug: an in-game toggle then did nothing until the player re-entered the world.
     *
     * @return {@code true} when the global list was written; {@code false} for a bad argument or an
     *         IO failure, so the caller can leave the switch where it was instead of lying.
     */
    public static boolean setActive(File gameDataDir, String uuid, String version, boolean active) {
        if (gameDataDir == null || uuid == null || uuid.trim().isEmpty()) return false;
        String key = uuid.trim().toLowerCase(Locale.ROOT);

        // The world files are the ones that matter live; write them first so a failure on the
        // global list cannot leave the running world out of sync with what the player just chose.
        for (File worldPacks : worldPackFiles(gameDataDir)) {
            mergeInto(worldPacks, key, version, active);
        }

        File global = globalFile(gameDataDir);
        return mergeInto(global, key, version, active);
    }

    /**
     * Rewrites one pack list file, adding/removing the uuid and preserving unrelated entries.
     *
     * @return true when the file was written (or does not need to exist for a removal).
     */
    private static boolean mergeInto(File file, String key, String version, boolean active) {
        if (file == null) return false;
        if (!active && !file.isFile()) {
            // Nothing to remove from a world that never had a lists file; leave it absent rather
            // than creating an empty one the game would then have to parse.
            return true;
        }

        List<JsonObject> kept = new ArrayList<>();
        JsonArray existing = readGlobalArray(file);
        if (existing != null) {
            for (JsonElement element : existing) {
                if (!element.isJsonObject()) continue;
                JsonObject object = element.getAsJsonObject();
                if (!object.has("pack_id")) continue;
                String packId = object.get("pack_id").getAsString().trim().toLowerCase(Locale.ROOT);
                if (packId.equals(key)) continue;
                // Re-emit with the version as an array: a string version (what this writer used to
                // produce) makes the game fail to match the pack and drop it silently.
                JsonObject clean = new JsonObject();
                clean.addProperty("pack_id", packId);
                clean.add("version", versionArray(versionOf(object)));
                kept.add(clean);
            }
        }

        if (active) {
            JsonObject entry = new JsonObject();
            entry.addProperty("pack_id", key);
            entry.add("version", versionArray(version));
            kept.add(entry);
        }

        try {
            writeGlobal(file, kept);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * The active uuids in the order the global list stores them, first to last.
     *
     * <p>This is the order a reorder starts from: reading it back rather than re-deriving it means
     * a move works from what the game will actually apply, not from the list's display order.
     * Returns an empty list when there is no list to read.
     */
    public static List<String> activeOrder(File gameDataDir) {
        List<String> result = new ArrayList<>();
        if (gameDataDir == null) return result;
        JsonArray array = readGlobalArray(globalFile(gameDataDir));
        if (array == null) return result;
        for (JsonElement element : array) {
            if (!element.isJsonObject()) continue;
            JsonObject object = element.getAsJsonObject();
            if (!object.has("pack_id")) continue;
            String key = object.get("pack_id").getAsString().trim().toLowerCase(Locale.ROOT);
            if (!key.isEmpty() && !result.contains(key)) result.add(key);
        }
        return result;
    }

    /**
     * Rewrites the global pack list so the active packs appear in {@code order}.
     *
     * <p>Bedrock applies active packs bottom-up, so the array order <em>is</em> the priority: the
     * last entry wins a texture conflict. The list is rebuilt from the current file, reordering the
     * active entries to match {@code order} and keeping every inactive entry (and any entry the
     * order does not name) in place, so a reorder can never drop a pack the game has applied.
     *
     * @param gameDataDir the instance's game-data root
     * @param order       the active uuids in the order they should appear in the list, first to
     *                    last; Bedrock applies the <em>last</em> entry with the highest priority,
     *                    so the last id here is the one that wins a texture conflict
     * @return true when the global list was written; false when there is no list to reorder, the
     *         argument is bad, or the write failed
     */
    public static boolean reorderActive(File gameDataDir, java.util.List<String> order) {
        if (gameDataDir == null) return false;

        // Index the desired rank by normalised uuid once, then apply it to every list file.
        java.util.Map<String, Integer> rank = new java.util.HashMap<>();
        if (order != null) {
            int next = 0;
            for (String id : order) {
                String key = ResourcePackOrder.normalise(id);
                if (!key.isEmpty() && !rank.containsKey(key)) rank.put(key, next++);
            }
        }

        File global = globalFile(gameDataDir);
        if (global == null || !global.isFile()) return false;

        // The world files are what the running session reads, so reorder them first: a failure on
        // the global list then cannot leave the running world in the old priority.
        boolean worldOk = true;
        for (File worldPacks : worldPackFiles(gameDataDir)) {
            if (worldPacks.isFile() && !reorderFile(worldPacks, rank)) worldOk = false;
        }

        return reorderFile(global, rank) && worldOk;
    }

    /**
     * Rewrites one pack list so the entries the rank names come first, in rank order, followed by
     * every other entry in its existing position.
     *
     * @return true when the file was written or did not exist; false on an IO failure
     */
    private static boolean reorderFile(File file, java.util.Map<String, Integer> rank) {
        JsonArray existing = readGlobalArray(file);
        if (existing == null) {
            // Nothing to reorder in a file that does not exist or is unreadable; leaving it absent
            // is correct, not a failure.
            return !file.isFile();
        }
        List<JsonObject> ranked = new ArrayList<>();
        List<JsonObject> others = new ArrayList<>();
        for (JsonElement element : existing) {
            if (!element.isJsonObject()) continue;
            JsonObject object = element.getAsJsonObject();
            if (!object.has("pack_id")) continue;
            String key = object.get("pack_id").getAsString().trim().toLowerCase(Locale.ROOT);
            JsonObject clean = new JsonObject();
            clean.addProperty("pack_id", key);
            clean.add("version", versionArray(versionOf(object)));
            if (rank.containsKey(key)) {
                ranked.add(clean);
            } else {
                others.add(clean);
            }
        }
        ranked.sort(java.util.Comparator.comparingInt(
                entry -> rank.getOrDefault(
                        entry.get("pack_id").getAsString().toLowerCase(Locale.ROOT), 0)));
        List<JsonObject> merged = new ArrayList<>(ranked);
        merged.addAll(others);
        try {
            writeGlobal(file, merged);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Asks the running session to pick up a pack change immediately.
     *
     * <p>The shipped Bedrock builds have no verified in-place refresh: the preloader export
     * resolves to "no hook" and returns false, so the caller relaunches. The native side detects
     * support at runtime — it marks the running build supported only after a refresh it confirmed,
     * and never retries a failed call in the same session — so there is no version list here to
     * keep in step.
     *
     * @return true when a live reload happened; false when no session is running or the running
     *         build could not refresh, in which case the change applies on the next world load or
     *         the caller relaunches.
     */
    public static boolean requestReload() {
        Reloader current = reloader;
        if (current == null) return false;
        try {
            return current.reloadPacks();
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Asks the running session to relaunch the instance so the new pack list takes effect.
     *
     * @return true when a relaunch was started; false when no session is running, in which case the
     *         change applies on the next world load.
     */
    public static boolean requestRestart() {
        Restarter current = restarter;
        if (current == null) return false;
        try {
            return current.restart();
        } catch (Throwable t) {
            return false;
        }
    }

    /** True when a running session can apply a pack change now (live reload or relaunch). */
    public static boolean hasLiveSession() {
        return reloader != null || restarter != null;
    }

    /** What happened when a pack change was applied, so the panel can say it honestly. */
    public enum ApplyOutcome {
        /** Written and the game refreshed in place. */
        RELOADED,
        /** Written and the running instance is relaunching so the change takes effect. */
        RESTARTING,
        /** Written; it applies the next time the world is loaded. */
        NEXT_LOAD,
        /** Nothing was written; the caller must not flip the switch. */
        FAILED
    }

    /**
     * Applies a pack change across every candidate game-data root and reports the outcome.
     *
     * <p>All roots are written because the game picks its storage from isolation and
     * internal/external, and a write to only the wrong guess is silently ignored. The live paths
     * are then attempted in order: an in-place reload if the session supports it, otherwise a
     * relaunch, otherwise the change is honestly reported as applying on the next load.
     */
    public static ApplyOutcome apply(List<File> gameDataDirs, String uuid, String version,
                                     boolean active) {
        boolean wrote = false;
        if (gameDataDirs != null) {
            for (File dir : gameDataDirs) {
                wrote |= setActive(dir, uuid, version, active);
            }
        }
        if (!wrote) return ApplyOutcome.FAILED;
        if (requestReload()) return ApplyOutcome.RELOADED;
        if (requestRestart()) return ApplyOutcome.RESTARTING;
        return ApplyOutcome.NEXT_LOAD;
    }

    /** Every world's own resource-list file under {@code minecraftWorlds/}. */
    public static List<File> worldPackFiles(File gameDataDir) {
        List<File> files = new ArrayList<>();
        if (gameDataDir == null) return files;
        File worlds = new File(gameDataDir, MINECRAFT_WORLDS_DIR);
        File[] children = worlds.listFiles(File::isDirectory);
        if (children == null) return files;
        for (File world : children) {
            files.add(new File(world, WORLD_RESOURCE_PACKS));
        }
        return files;
    }

    private static File globalFile(File gameDataDir) {
        if (gameDataDir == null) return null;
        return new File(new File(gameDataDir, MINECRAFT_PE_DIR), GLOBAL_RESOURCE_PACKS);
    }

    /**
     * Coerces a version to the JSON form the game expects: a three-number array ({@code [1,0,0]}).
     *
     * <p>The pack files are matched on this value, and a <em>string</em> version is not comparable
     * to the manifest's array, so the entry is dropped and the pack never activates. Everything
     * this class writes therefore goes through here, and a legacy string version already on disk is
     * normalised on the next rewrite.
     */
    private static JsonArray versionArray(String version) {
        int[] parts = {1, 0, 0};
        if (version != null && !version.trim().isEmpty()) {
            String[] tokens = version.trim().split("\\.");
            for (int i = 0; i < 3 && i < tokens.length; i++) {
                try {
                    parts[i] = Integer.parseInt(tokens[i].trim());
                } catch (NumberFormatException ignored) {
                    parts[i] = 0;
                }
            }
        }
        JsonArray array = new JsonArray();
        array.add(parts[0]);
        array.add(parts[1]);
        array.add(parts[2]);
        return array;
    }

    /** Reads a version entry whether it was written as a string or an array (or is absent). */
    private static String versionOf(JsonObject entry) {
        if (!entry.has("version")) return null;
        JsonElement value = entry.get("version");
        if (value.isJsonArray()) {
            JsonArray array = value.getAsJsonArray();
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < array.size(); i++) {
                if (i > 0) builder.append('.');
                builder.append(array.get(i).getAsInt());
            }
            return builder.toString();
        }
        return value.getAsString();
    }

    private static JsonArray readGlobalArray(File global) {
        if (global == null || !global.isFile()) return null;
        try (FileReader reader = new FileReader(global)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (parsed != null && parsed.isJsonArray()) {
                return parsed.getAsJsonArray();
            }
        } catch (Exception ignored) {
            AppLog.warnOnce("CaramelClient", "InGamePackChanger:525", "Ignored a failure at InGamePackChanger:525", ignored);
        }
        return null;
    }

    private static void writeGlobal(File global, List<JsonObject> entries) throws IOException {
        File parent = global.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs() && !parent.isDirectory()) {
            throw new IOException("Failed to create " + parent);
        }
        JsonArray array = new JsonArray();
        for (JsonObject entry : entries) array.add(entry);

        File temp = new File(parent, global.getName() + ".chimeralauncher_tmp");
        try (FileWriter writer = new FileWriter(temp, false)) {
            GSON.toJson(array, writer);
        }
        try {
            Files.move(temp.toPath(), global.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Files.move(temp.toPath(), global.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** The human-readable pack name from its manifest header, falling back to the folder name. */
    private static String readPackName(File pack) {
        try {
            File manifest = pack.isDirectory()
                    ? new File(pack, "manifest.json")
                    : null;
            if (manifest != null && manifest.isFile()) {
                try (FileReader reader = new FileReader(manifest)) {
                    JsonObject document = JsonParser.parseReader(reader).getAsJsonObject();
                    JsonObject header = document.getAsJsonObject("header");
                    if (header != null && header.has("name")) {
                        String name = header.get("name").getAsString().trim();
                        if (!name.isEmpty()) return name;
                    }
                }
            }
        } catch (Exception ignored) {
            AppLog.warnOnce("CaramelClient", "InGamePackChanger:567", "Ignored a failure at InGamePackChanger:567", ignored);
        }
        return pack.getName();
    }

    /** Convenience for callers that only need the count of active packs. */
    public static int activeCount(File gameDataDir) {
        return activeUuids(gameDataDir).size();
    }

    /** Unmodifiable empty list, so callers can avoid a null check. */
    public static List<PackEntry> emptyList() {
        return Collections.emptyList();
    }
}
