package org.chimeramc.client.core.antegg;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The read model behind the script manager screen.
 *
 * <p>The manager lists what is installed in the {@code .AntEgg} sandbox — not just scripts, but
 * every package, because a native package and a script package live side by side and the user
 * wants one list — with the load order the loader uses and each package's declared dependencies
 * resolved against the sandbox so an unmet one is visible before a launch rather than only as a
 * load failure.
 *
 * <p>It is pure and file-based (no Android types) so it is unit-tested on the JVM: the screen
 * renders what {@link #list} returns, and the same values are what the tests assert.
 */
public final class ScriptManager {

    private ScriptManager() {}

    /**
     * One installed package as the manager shows it.
     *
     * <p>{@link #id} is the sandbox directory name; {@link #readable} is false for a directory
     * whose manifest could not be read (a half-removed package, or a foreign folder), which the
     * screen shows as a row it can remove rather than hiding — a broken package the user cannot see
     * is a package they cannot delete.
     */
    public static final class ScriptInfo {
        public final String id;
        public final String name;
        public final String version;
        public final String author;
        public final String type;
        public final String entryPoint;
        public final boolean readable;
        public final List<DependencyStatus> dependencies;
        public final int loadIndex;

        ScriptInfo(String id, String name, String version, String author, String type,
                   String entryPoint, boolean readable, List<DependencyStatus> dependencies,
                   int loadIndex) {
            this.id = id;
            this.name = name;
            this.version = version;
            this.author = author;
            this.type = type;
            this.entryPoint = entryPoint;
            this.readable = readable;
            this.dependencies = dependencies == null
                    ? Collections.emptyList() : Collections.unmodifiableList(dependencies);
            this.loadIndex = loadIndex;
        }

        /** True when the manifest declares this package runs a script. */
        public boolean isScript() {
            return "script".equals(type);
        }

        /** True when every declared dependency is satisfied by the sandbox. */
        public boolean allDependenciesMet() {
            for (DependencyStatus status : dependencies) {
                if (!status.met) return false;
            }
            return true;
        }
    }

    /**
     * One declared dependency of a package, resolved against what the sandbox contains.
     *
     * <p>{@link #met} is false for a dependency that is absent or whose installed version does not
     * satisfy {@link #range}; {@link #installedVersion} carries what was found (null when absent)
     * so the row can say which half failed, the same distinction {@code AntEggLoader} reports at
     * load time.
     */
    public static final class DependencyStatus {
        public final String id;
        public final String range;
        public final String installedVersion;
        public final boolean met;

        DependencyStatus(String id, String range, String installedVersion, boolean met) {
            this.id = id;
            this.range = range;
            this.installedVersion = installedVersion;
            this.met = met;
        }
    }

    /** Lists every package in the sandbox, ordered the way the loader loads them. */
    public static List<ScriptInfo> list(File sandboxRoot) {
        List<ScriptInfo> out = new ArrayList<>();
        if (sandboxRoot == null || !sandboxRoot.isDirectory()) return out;

        List<String> ids = AntEggPackage.installedIds(sandboxRoot);
        Collections.sort(ids);
        for (String id : ids) {
            File dir = new File(sandboxRoot, id);
            AntEggPackage.Result read = AntEggPackage.readExtracted(dir);
            if (read == null || !read.isValid() || read.manifest == null) {
                out.add(new ScriptInfo(id, id, "", "", "", "", false,
                        Collections.emptyList(), 0));
                continue;
            }
            AntEggManifest manifest = read.manifest;
            List<DependencyStatus> deps = resolveDependencies(sandboxRoot, manifest);
            out.add(new ScriptInfo(manifest.id(), manifest.name, manifest.version, manifest.author,
                    manifest.type, manifest.entryPoint, true, deps, 0));
        }

        applyLoadOrder(out);
        return out;
    }

    /**
     * Resolves each declared dependency against the sandbox.
     *
     * <p>A dependency entry that cannot be parsed is reported unmet rather than dropped, matching
     * {@code AntEggLoader}: a manifest the format cannot express must not look satisfied.
     */
    static List<DependencyStatus> resolveDependencies(File sandboxRoot, AntEggManifest manifest) {
        List<DependencyStatus> out = new ArrayList<>();
        if (manifest == null) return out;
        for (String entry : manifest.dependencies) {
            DependencyRef ref = DependencyRef.parse(entry);
            if (ref == null || ref.id.isEmpty()) {
                out.add(new DependencyStatus(entry == null ? "" : entry, "", null, false));
                continue;
            }
            String installed = AntEggPackage.versionOfInstalled(sandboxRoot, ref.id);
            boolean met;
            if (installed == null) {
                met = false;
            } else if (ref.range == null || ref.range.isAny()) {
                met = true;
            } else {
                met = ref.range.matches(installed);
            }
            String range = ref.range == null ? "*" : ref.range.describe();
            out.add(new DependencyStatus(ref.id, range, installed, met));
        }
        return out;
    }

    /** The outcome of re-running one script, so the screen can report per-package results. */
    public static final class RunOutcome {
        public final String id;
        public final boolean success;
        public final String error;

        RunOutcome(String id, boolean success, String error) {
            this.id = id;
            this.success = success;
            this.error = error;
        }
    }

    /**
     * True when a row should be run: a readable script package whose id is not in the disabled set.
     *
     * <p>Pure, so the enable/disable rule is unit-tested without the Lua VM. A native package is
     * never "run" here — its entry point is injected by the loader at import, not re-executed — and
     * an unreadable row has no manifest to run, so both are skipped rather than reported as
     * failures.
     */
    public static boolean shouldRun(ScriptInfo info, java.util.Set<String> disabledIds) {
        if (info == null || !info.readable || !info.isScript()) return false;
        return disabledIds == null || !disabledIds.contains(info.id);
    }

    /**
     * Re-runs every enabled script in the sandbox, returning one outcome per script attempted.
     *
     * <p>This is the "reload" the manager offers: an .AntEgg script is a setup script, so re-running
     * it re-applies whatever it configures without re-importing the package. A script that throws
     * is reported with its error and the others still run, so one bad script does not block the
     * reload.
     */
    public static List<RunOutcome> runEnabled(File sandboxRoot, java.util.Set<String> disabledIds) {
        List<RunOutcome> out = new ArrayList<>();
        if (sandboxRoot == null) return out;
        for (ScriptInfo info : list(sandboxRoot)) {
            if (!shouldRun(info, disabledIds)) continue;
            File dir = new File(sandboxRoot, info.id);
            AntEggPackage.Result read = AntEggPackage.readExtracted(dir);
            if (read == null || !read.isValid() || read.manifest == null) continue;
            AntEggScriptHost.Result result = AntEggScriptHost.run(dir, read.manifest);
            out.add(new RunOutcome(info.id, result.success, result.error));
        }
        return out;
    }

    /**
     * Fills in each row's {@link ScriptInfo#loadIndex} from the loader's own ordering, so the list
     * shows the order a batch import would install them in rather than an alphabetical order that
     * disagrees with the loader.
     */
    private static void applyLoadOrder(List<ScriptInfo> infos) {
        List<AntEggLoadOrder.Entry> entries = new ArrayList<>(infos.size());
        for (int i = 0; i < infos.size(); i++) {
            ScriptInfo info = infos.get(i);
            List<String> dependencyIds = new ArrayList<>();
            for (DependencyStatus status : info.dependencies) {
                if (status.id != null && !status.id.isEmpty()) {
                    dependencyIds.add(status.id);
                }
            }
            entries.add(new AntEggLoadOrder.Entry(i, info.id, dependencyIds));
        }
        int position = 0;
        for (int index : AntEggLoadOrder.order(entries)) {
            ScriptInfo info = infos.get(index);
            infos.set(index, new ScriptInfo(info.id, info.name, info.version, info.author,
                    info.type, info.entryPoint, info.readable, info.dependencies, position++));
        }
    }
}
