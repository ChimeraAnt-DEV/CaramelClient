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
 * A text-bearing view does not pin its height to a dp value (item 340).
 *
 * <p>A fixed height does not grow with the user's font-size setting, so at 200% the label is
 * clipped. The layout rule is {@code wrap_content} plus {@code android:minHeight}: the view is at
 * least the intended size and grows when the text needs it.
 *
 * <p>This is the CI half of the item; walking every screen at 200% on a device is the other half,
 * and is recorded as a gap in {@code docs/ACCESSIBILITY.md}.
 */
public class FontScaleLayoutTest {

    /** A text-bearing widget. Buttons are included: they have labels too. */
    private static final Pattern TEXT_VIEW = Pattern.compile(
            "<(TextView|Button|androidx\\.appcompat\\.widget\\.AppCompatButton"
                    + "|com\\.google\\.android\\.material\\.button\\.MaterialButton"
                    + "|androidx\\.appcompat\\.widget\\.AppCompatTextView)\\b[^>]*?/?>",
            Pattern.DOTALL);

    private static final Pattern FIXED_HEIGHT =
            Pattern.compile("android:layout_height=\"([0-9]+)dp\"");

    @Test
    public void noTextViewPinsItsHeightToADpValue() throws IOException {
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(resolve("app/src/main/res/layout"))) {
            for (Path file : (Iterable<Path>) paths
                    .filter(p -> p.toString().endsWith(".xml"))::iterator) {
                String xml = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                Matcher m = TEXT_VIEW.matcher(xml);
                while (m.find()) {
                    Matcher h = FIXED_HEIGHT.matcher(m.group());
                    if (!h.find()) continue;
                    int line = xml.substring(0, m.start()).split("\n", -1).length;
                    offenders.add(file.getFileName() + ":" + line + " " + h.group(1) + "dp");
                }
            }
        }
        assertTrue("a text-bearing view must use wrap_content + android:minHeight, not a fixed"
                        + " layout_height: a fixed height clips the label at a large font scale."
                        + " Offenders:\n  " + String.join("\n  ", offenders),
                offenders.isEmpty());
    }

    private static Path resolve(String relativePath) {
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            Path candidate = dir.resolve(relativePath);
            if (Files.exists(candidate)) return candidate;
            dir = dir.getParent();
        }
        throw new IllegalStateException("could not find " + relativePath);
    }
}
