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
 * The layouts are direction-agnostic, so a right-to-left locale mirrors instead of breaking
 * (item 337).
 *
 * <p>{@code paddingLeft}/{@code paddingRight} (and the margin and compound-drawable forms) are
 * absolute: they stay on the physical side when the layout direction flips, so a row that reads
 * "icon, then label" in English reads "label, then icon pushed to the wrong edge" in Arabic or
 * Hebrew. The {@code *Start}/{@code *End} forms resolve against the layout direction and are the
 * only correct choice for anything that is not deliberately physical.
 *
 * <p>There is no pseudo-locale device test in this repo, so this scan is the automatic gate the
 * plan asks for: it reads the real layout files and fails on a reintroduction rather than waiting
 * for a screenshot from an RTL device.
 */
public class RtlLayoutTest {

    private static final Path LAYOUT_ROOT = Paths.get("src", "main", "res");

    /**
     * The absolute direction attributes. {@code drawableLeft}/{@code drawableRight} are included
     * because they position a compound drawable on a fixed side for the same reason.
     */
    private static final Pattern ABSOLUTE_DIRECTION = Pattern.compile(
            "android:(?:layout_)?(?:padding|margin)(?:Left|Right)\\b"
                    + "|android:(?:drawable|compoundDrawable)(?:Left|Right)\\b");

    @Test
    public void noLayoutPinsToAPhysicalSide() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (Path file : layoutFiles()) {
            String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            Matcher m = ABSOLUTE_DIRECTION.matcher(text);
            while (m.find()) {
                offenders.add(file.getFileName() + " -> " + m.group());
            }
        }
        assertTrue("a layout must use paddingStart/End, marginStart/End and drawableStart/End so it "
                + "mirrors in an RTL locale; a physical Left/Right stays on the wrong side. Offenders: "
                + offenders, offenders.isEmpty());
    }

    private static List<Path> layoutFiles() throws IOException {
        List<Path> out = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(LAYOUT_ROOT)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".xml"))
                    .filter(p -> p.getParent().getFileName().toString().startsWith("layout"))
                    .forEach(out::add);
        }
        return out;
    }
}
