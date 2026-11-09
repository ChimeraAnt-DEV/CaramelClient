package org.chimeramc.client.ui;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * The client uses no emoji icons in its own text.
 *
 * <p>Emoji render inconsistently across Android versions and fonts, so a glyph that looks right on
 * one device is a tofu box on another. The client draws its icons as vector drawables and its type
 * as the bundled fonts, so no emoji should appear in a string resource, a layout or a source
 * literal. This is a guard: it fails the build the moment one is introduced, rather than waiting for
 * a report of a missing glyph.
 *
 * <p>Only pictographic ranges are checked. A plain arrow or a mathematical symbol (a "→" in a code
 * comment, say) is not an emoji and is left alone.
 */
public class NoEmojiInUiTextTest {

    /** Ranges that are emoji or emoji presentation, not typographic symbols. */
    private static boolean isEmoji(int cp) {
        return (cp >= 0x1F000 && cp <= 0x1FAFF)   // pictographs, faces, objects, symbols
                || (cp >= 0x2600 && cp <= 0x27BF) // misc symbols and dingbats
                || (cp >= 0x2B00 && cp <= 0x2BFF) // arrows/stars used as emoji
                || (cp >= 0x1F1E6 && cp <= 0x1F1FF) // regional indicators (flags)
                || cp == 0xFE0F;                  // variation selector-16 (emoji presentation)
    }

    @Test
    public void noEmojiInResources() throws IOException {
        assertNoEmoji(new String[]{"app/src/main/res"}, ".xml");
    }

    @Test
    public void noEmojiInSources() throws IOException {
        assertNoEmoji(new String[]{"app/src/main/java"}, ".java", ".kt");
    }

    private void assertNoEmoji(String[] roots, String... extensions) throws IOException {
        List<String> offenders = new ArrayList<>();
        for (String relative : roots) {
            Path dir = root().resolve(relative);
            if (!Files.isDirectory(dir)) continue;
            try (Stream<Path> walk = Files.walk(dir)) {
                walk.filter(Files::isRegularFile)
                        .filter(p -> hasExtension(p, extensions))
                        .forEach(p -> collect(p, offenders));
            }
        }
        assertTrue("emoji icons are not allowed in client text; found in: " + offenders,
                offenders.isEmpty());
    }

    private static boolean hasExtension(Path path, String[] extensions) {
        String name = path.getFileName().toString();
        for (String extension : extensions) {
            if (name.endsWith(extension)) return true;
        }
        return false;
    }

    private static void collect(Path path, List<String> offenders) {
        try {
            String text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            for (int i = 0; i < text.length(); ) {
                int cp = text.codePointAt(i);
                if (isEmoji(cp)) {
                    offenders.add(path.getFileName() + " U+" + Integer.toHexString(cp).toUpperCase());
                    return;
                }
                i += Character.charCount(cp);
            }
        } catch (IOException ignored) {
            // An unreadable file cannot introduce an emoji the guard would miss in review.
        }
    }

    private static Path root() {
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            if (Files.exists(dir.resolve("app/src/main/res/layout"))) return dir;
            dir = dir.getParent();
        }
        throw new IllegalStateException("could not find the project root");
    }
}
