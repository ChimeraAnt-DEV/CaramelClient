package org.chimeramc.client.ui.components;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.Test;

/**
 * Every {@code ImageView} declares whether it is content or decoration.
 *
 * <p>TalkBack announces an image by its {@code contentDescription}; an image with neither a
 * description nor an explicit {@code importantForAccessibility="no"} is announced as an unlabelled
 * node, which is worse than silence because it makes the user stop and interrogate every row. The
 * audit found 39 such views; the rule is that an image must be one of two things:
 * labelled, or deliberately marked decorative.
 *
 * <p>The check scans the real layout tree, so a new unlabelled image fails the build rather than
 * being found by a later audit.
 */
public class ImageViewAccessibilityTest {

    private static final Path LAYOUT_ROOT = Paths.get("src", "main", "res", "layout");

    private static final Pattern IMAGE_VIEW = Pattern.compile("<ImageView\\b[^>]*?/?>", Pattern.DOTALL);

    @Test
    public void everyImageViewIsLabelledOrDecorative() throws IOException {
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(LAYOUT_ROOT)) {
            for (Path file : (Iterable<Path>) paths
                    .filter(p -> p.toString().endsWith(".xml"))::iterator) {
                String source = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                Matcher matcher = IMAGE_VIEW.matcher(source);
                while (matcher.find()) {
                    String tag = matcher.group();
                    if (tag.contains("contentDescription")
                            || tag.contains("importantForAccessibility")) {
                        continue;
                    }
                    int line = source.substring(0, matcher.start()).split("\n", -1).length;
                    offenders.add(LAYOUT_ROOT.relativize(file) + ":" + line);
                }
            }
        }
        assertTrue("an ImageView must carry a contentDescription (content) or"
                        + " importantForAccessibility=\"no\" (decoration); an unlabelled image is an"
                        + " unnamed node in TalkBack. Offenders:\n  "
                        + String.join("\n  ", offenders), offenders.isEmpty());
    }
}
