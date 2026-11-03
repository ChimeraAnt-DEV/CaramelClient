package org.chimeramc.client.ui.components;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

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

/**
 * The UI glyph set is drawn to one spec: square, one rendered stroke weight, round joins.
 *
 * <p>The set had grown nine different stroke widths (0.5 to 2) and seven icons whose strokes ended in
 * square caps, so two icons side by side read as different weights and the line ends looked chipped.
 *
 * <p>What matters visually is the stroke's <em>rendered</em> weight, not its raw value: an icon on a
 * 48px viewport at stroke 2 draws the same 2dp line as a 24px viewport at stroke 2. So the rule
 * measures {@code strokeWidth / viewport * width} and requires one of the two steps. Brand artwork
 * (the multi-colour GitHub/Patreon/YouTube marks), the adaptive launcher layers and the large
 * decorative images are not glyphs on this grid and are excluded by name.
 */
public class IconSetConsistencyTest {

    private static final Pattern STROKE_WIDTH = Pattern.compile("android:strokeWidth=\"([0-9.]+)\"");

    private static final List<String> NON_GLYPH_EXCLUSIONS = List.of(
            "ic_github.xml", "ic_patreon.xml", "ic_youtube.xml",
            "ic_launcher_background.xml", "ic_server.xml", "ic_server_star.xml",
            "ic_behavior.xml", "ic_minecraft_cube.xml", "ic_cursor_overlay.xml");

    @Test
    public void everyGlyphIsSquareAndDrawsTheSameRenderedStroke() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (Path p : icons()) {
            String xml = read(p);
            float width = dimen(xml, "android:width");
            float height = dimen(xml, "android:height");
            if (width <= 0f || height <= 0f) {
                offenders.add(p.getFileName() + " has no explicit width/height");
                continue;
            }
            if (Math.abs(width - height) > 0.01f) {
                offenders.add(p.getFileName() + " is not square (" + width + "x" + height + ")");
            }
            float viewport = dimen(xml, "android:viewportWidth");
            if (viewport <= 0f) {
                offenders.add(p.getFileName() + " has no viewport width");
                continue;
            }
            Matcher m = STROKE_WIDTH.matcher(xml);
            while (m.find()) {
                float rendered = Float.parseFloat(m.group(1)) / viewport * width;
                if (!isAllowedRenderedStroke(rendered)) {
                    offenders.add(p.getFileName() + " draws a " + rendered + "dp stroke (from "
                            + m.group(1) + " on a " + viewport + " viewport)");
                }
            }
        }
        assertTrue("a glyph must be square and draw a 2dp or 1.5dp stroke so it matches its "
                + "neighbours: " + offenders, offenders.isEmpty());
    }

    @Test
    public void everyStrokeHasRoundCapsAndJoins() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (Path p : icons()) {
            String xml = read(p);
            if (!xml.contains("android:strokeWidth")) continue;
            if (!xml.contains("android:strokeLineCap=\"round\"")
                    || !xml.contains("android:strokeLineJoin=\"round\"")) {
                offenders.add(p.getFileName().toString());
            }
        }
        assertTrue("a stroked icon must set round caps and joins, or its line ends look chipped: "
                + offenders, offenders.isEmpty());
    }

    /** The rendered stroke is one of the two steps, within a small rounding tolerance. */
    private static boolean isAllowedRenderedStroke(float renderedDp) {
        return Math.abs(renderedDp - 2f) < 0.12f || Math.abs(renderedDp - 1.5f) < 0.12f;
    }

    private static float dimen(String xml, String attribute) {
        Matcher m = Pattern.compile(attribute + "=\"([0-9.]+)(?:dp)?\"").matcher(xml);
        return m.find() ? Float.parseFloat(m.group(1)) : 0f;
    }

    private static List<Path> icons() throws IOException {
        Path dir = projectRoot().resolve("app/src/main/res/drawable");
        List<Path> out = new ArrayList<>();
        try (Stream<Path> s = Files.list(dir)) {
            s.filter(p -> p.getFileName().toString().startsWith("ic_"))
                    .filter(p -> p.getFileName().toString().endsWith(".xml"))
                    .filter(p -> !NON_GLYPH_EXCLUSIONS.contains(p.getFileName().toString()))
                    .sorted().forEach(out::add);
        }
        return out;
    }

    private static String read(Path p) throws IOException {
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }

    private static Path projectRoot() {
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            if (Files.exists(dir.resolve("app/src/main/res/layout"))) return dir;
            dir = dir.getParent();
        }
        throw new IllegalStateException("could not find the project root");
    }
}
