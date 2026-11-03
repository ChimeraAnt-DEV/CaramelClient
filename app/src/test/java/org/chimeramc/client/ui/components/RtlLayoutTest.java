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
 *.
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
     * The absolute direction attributes. Each one names a physical side, so it stays put when the
     * layout direction flips.
     *
     * <ul>
     *   <li>{@code padding}/{@code margin} {@code Left}/{@code Right} and the
     *       {@code drawable}/{@code compoundDrawable} pair - the {@code *Start}/{@code *End} forms
     *       resolve against the direction.
     *   <li>{@code gravity="left|right"} and the layout-gravity form - {@code start}/{@code end}
     *       are the direction-relative equivalents.
     *   <li>{@code textAlignment="viewLeft|viewRight"} - {@code viewStart}/{@code viewEnd}.
     *   <li>the {@code RelativeLayout} rules ({@code layout_toLeftOf}, {@code layout_alignRight},
     *       {@code layout_alignParentStart}'s physical twin) and the {@code ConstraintLayout}
     *       {@code layout_constraintLeft_to*} form.
     * </ul>
     *
     * <p>{@code textAlignment="textStart"} is deliberately <em>not</em> reported: it is the
     * direction-relative value (the one the framework uses when the attribute is absent).
     */
    private static final Pattern ABSOLUTE_DIRECTION = Pattern.compile(
            "android:(?:layout_)?(?:padding|margin)(?:Left|Right)\\b"
                    + "|android:(?:drawable|compoundDrawable)(?:Left|Right)\\b"
                    + "|android:(?:layout_)?gravity=\"[^\"]*\\b(?:left|right)\\b"
                    + "|android:textAlignment=\"(?:viewLeft|viewRight)\""
                    + "|android:layout_(?:toLeftOf|toRightOf|alignLeft|alignRight"
                    + "|alignParentLeft|alignParentRight)="
                    + "|app:layout_constraint(?:Left|Right)_to");

    @Test
    public void noLayoutPinsToAPhysicalSide() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (Path file : layoutFiles()) {
            String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            Matcher m = ABSOLUTE_DIRECTION.matcher(text);
            while (m.find()) {
                int line = text.substring(0, m.start()).split("\n", -1).length;
                offenders.add(file.getFileName() + ":" + line + " -> " + m.group());
            }
        }
        assertTrue("a layout must use the Start/End forms so it mirrors in an RTL locale; a physical "
                + "Left/Right stays on the wrong side. Offenders: " + offenders, offenders.isEmpty());
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
