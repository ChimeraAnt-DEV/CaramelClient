package org.chimeramc.client.ui.components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the heading description rule and the layout invariant behind it.
 *
 * <p>{@link UiAccessibility#headingDescription} is pure, so it is tested directly. The layout half
 * reads the real resource files: a section title that is not marked as a heading is exactly the
 * regression this item is about, and it is invisible on a build machine without a test that looks
 * for it.
 */
public class UiAccessibilityTest {

    @Test
    public void aHeadingWithASubtitleJoinsThemWithAPeriod() {
        assertEquals("Mods. Manage installed mods",
                UiAccessibility.headingDescription("Mods", "Manage installed mods"));
    }

    @Test
    public void aHeadingWithoutASubtitleIsJustTheTitle() {
        assertEquals("Mods", UiAccessibility.headingDescription("Mods", null));
        assertEquals("Mods", UiAccessibility.headingDescription("Mods", "   "));
    }

    @Test
    public void blankTitlesProduceNoDescription() {
        assertEquals("", UiAccessibility.headingDescription(null, "x"));
        assertEquals("", UiAccessibility.headingDescription("", "x"));
        assertEquals("", UiAccessibility.headingDescription("   ", "x"));
    }

    @Test
    public void titlesAndSubtitlesAreTrimmed() {
        assertEquals("A. B", UiAccessibility.headingDescription("  A  ", "  B  "));
    }

    @Test
    public void everySectionTitleInAScreenLayoutIsMarkedAsAHeading() throws java.io.IOException {
        // The regression this item exists for: a section title that is not a heading, which a
        // screen-reader user can only reach by swiping through every row above it.
        java.util.List<String> offenders = new java.util.ArrayList<>();
        java.util.regex.Pattern title = java.util.regex.Pattern.compile(
                "<TextView\\b[^>]*?/>", java.util.regex.Pattern.DOTALL);
        for (java.nio.file.Path layout : screenLayouts()) {
            String xml = read(layout);
            java.util.regex.Matcher m = title.matcher(xml);
            while (m.find()) {
                String block = m.group();
                boolean isTitle = block.contains("TextAppearance.Glowberry.Title")
                        && block.contains("android:textStyle=\"bold\"");
                if (isTitle && !block.contains("accessibilityHeading")) {
                    offenders.add(layout.getFileName() + ": " + firstLine(block));
                }
            }
        }
        assertTrue("every section title in an activity or dialog layout must carry "
                + "android:accessibilityHeading=\"true\" so TalkBack can jump between sections. "
                + "Offenders: " + offenders, offenders.isEmpty());
    }

    private static String firstLine(String block) {
        int end = block.indexOf('\n');
        return end < 0 ? block : block.substring(0, end);
    }

    private static java.util.List<java.nio.file.Path> screenLayouts() throws java.io.IOException {
        java.nio.file.Path dir = resolve("app/src/main/res/layout");
        java.util.List<java.nio.file.Path> out = new java.util.ArrayList<>();
        try (java.util.stream.Stream<java.nio.file.Path> s = java.nio.file.Files.list(dir)) {
            s.filter(p -> {
                String name = p.getFileName().toString();
                return name.endsWith(".xml")
                        && (name.startsWith("activity_") || name.startsWith("dialog_"));
            }).sorted().forEach(out::add);
        }
        return out;
    }

    private static String read(java.nio.file.Path path) throws java.io.IOException {
        return new String(java.nio.file.Files.readAllBytes(path),
                java.nio.charset.StandardCharsets.UTF_8);
    }

    private static java.nio.file.Path resolve(String relativePath) {
        java.nio.file.Path dir = java.nio.file.Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            java.nio.file.Path candidate = dir.resolve(relativePath);
            if (java.nio.file.Files.exists(candidate)) return candidate;
            dir = dir.getParent();
        }
        throw new IllegalStateException("could not find " + relativePath);
    }

    @Test
    public void theSwitchNameRuleRecognisesTheAppSwitchTypes() {
        assertTrue(UiAccessibility.isSwitchClassName(
                "com.google.android.material.switchmaterial.SwitchMaterial"));
        assertTrue(UiAccessibility.isSwitchClassName("androidx.appcompat.widget.SwitchCompat"));
        assertTrue(UiAccessibility.isSwitchClassName("android.widget.Switch"));
        assertFalse(UiAccessibility.isSwitchClassName("android.widget.TextView"));
        assertFalse(UiAccessibility.isSwitchClassName(null));
    }
}
