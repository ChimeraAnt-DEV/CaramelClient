package org.chimeramc.client.preloader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The sweetloader/Minecraft compatibility table must match the rules the client actually ships
 *.
 *
 * <p>{@code docs/sweetloader-compatibility.md} is the page a user reads to answer "does my Minecraft
 * build work", and the answer comes from {@code sweetloader_signature_rules_source.json}. Two copies
 * of the same fact drift the moment a rule is added, so this reads the real files and fails when
 * they disagree. It is the same shape as the JNI entry-point table check: documentation that
 * nothing compiles against needs a test or it is wrong within a release.
 */
public class SweetLoaderCompatibilityDocTest {

    private static final Pattern ROW = Pattern.compile(
            "^\\|\\s*([0-9][0-9.]*)\\s*-\\s*([0-9][0-9.]*)\\s*\\|");

    @Test
    public void theDocTableListsExactlyTheShippedRuleRanges() throws IOException {
        Path repo = findRepoRoot();
        Set<String> shipped = shippedRanges(repo);
        Set<String> documented = documentedRanges(repo);

        assertEquals("the compatibility table and the shipped rules disagree. Shipped: " + shipped
                + " documented: " + documented, shipped, documented);
    }

    @Test
    public void theDocNamesTheShippedRevision() throws IOException {
        Path repo = findRepoRoot();
        JsonObject doc = JsonParser.parseString(read(repo.resolve(
                "resources/sweetloader/sweetloader_signature_rules_source.json"))).getAsJsonObject();
        String revision = doc.get("revision").getAsString();
        String text = read(repo.resolve("docs/sweetloader-compatibility.md"));
        assertTrue("the compatibility page must name the shipped rule revision (" + revision
                + "); a page that does not is a stale copy", text.contains("revision " + revision));
    }

    private static Set<String> shippedRanges(Path repo) throws IOException {
        JsonObject doc = JsonParser.parseString(read(repo.resolve(
                "resources/sweetloader/sweetloader_signature_rules_source.json"))).getAsJsonObject();
        JsonArray rules = doc.getAsJsonArray("rules");
        Set<String> ranges = new LinkedHashSet<>();
        for (JsonElement element : rules) {
            JsonObject rule = element.getAsJsonObject();
            String min = rule.has("min") ? rule.get("min").getAsString() : "";
            String max = rule.has("max") ? rule.get("max").getAsString() : min;
            ranges.add(min + " - " + max);
        }
        return ranges;
    }

    private static Set<String> documentedRanges(Path repo) throws IOException {
        String text = read(repo.resolve("docs/sweetloader-compatibility.md"));
        Set<String> ranges = new LinkedHashSet<>();
        for (String line : text.split("\\R")) {
            Matcher m = ROW.matcher(line);
            if (m.find()) ranges.add(m.group(1) + " - " + m.group(2));
        }
        assertTrue("the compatibility table parsed no rows; the check would pass vacuously",
                ranges.size() > 5);
        return ranges;
    }

    private static String read(Path path) throws IOException {
        assertTrue("missing file: " + path, Files.isRegularFile(path));
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private static Path findRepoRoot() {
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            if (Files.exists(dir.resolve("settings.gradle"))
                    && Files.isDirectory(dir.resolve("app/src/main/cpp/sweetloader"))) {
                return dir;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("repo root not found from " + Paths.get("").toAbsolutePath());
    }
}
