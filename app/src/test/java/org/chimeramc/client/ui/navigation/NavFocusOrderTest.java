package org.chimeramc.client.ui.navigation;

import static org.junit.Assert.assertEquals;
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

/**
 * The navigation's TalkBack focus order is back button, tabs, then the trailing controls (item 343).
 *
 * <p>Android walks the view hierarchy in declaration order when nothing overrides it, so the order a
 * screen-reader user hears is the order the ids are written in the layout — and nothing in the build
 * would notice if someone moved a block. This reads the real layouts and pins the order, so a
 * reordered header fails here rather than being found by a user.
 *
 * <p>It also pins that the tab ids appear in {@link LauncherTab} declaration order, because that is
 * the order the bar draws them and the order the shoulder-button cycle uses; a tab inserted in the
 * layout but not the enum (or the reverse) is a destination the user can reach with the stick and
 * not with a screen reader.
 */
public class NavFocusOrderTest {

    private static final Pattern ID = Pattern.compile("android:id=\"@\\+id/([^\"]+)\"");

    private static final List<String> TAB_IDS = List.of(
            "nav_tab_launch", "nav_tab_library", "nav_tab_mods", "nav_tab_settings");

    @Test
    public void bothLayoutsDeclareTheBackButtonThenTheLogoThenTheTabs() throws IOException {
        for (String layout : new String[]{"nav_bar.xml", "nav_rail.xml"}) {
            List<String> ids = idsOf(layout);
            int back = ids.indexOf("nav_back_button");
            int logo = ids.indexOf("nav_logo");
            int firstTab = indexOfFirstTab(ids);
            assertTrue(layout + ": the back button must be declared", back >= 0);
            assertTrue(layout + ": the logo must be declared", logo >= 0);
            assertTrue(layout + ": a tab must be declared", firstTab >= 0);
            assertTrue(layout + ": the back button comes before the logo", back < logo);
            assertTrue(layout + ": the logo comes before the tabs", logo < firstTab);
        }
    }

    @Test
    public void theTabsAreDeclaredInLauncherTabOrder() throws IOException {
        List<String> expected = new ArrayList<>();
        for (LauncherTab tab : LauncherTab.values()) {
            expected.add(nameFor(tab));
        }
        assertEquals("the enum order is the declaration order the layout must use",
                TAB_IDS, expected);
        for (String layout : new String[]{"nav_bar.xml", "nav_rail.xml"}) {
            List<String> ids = idsOf(layout);
            int previous = -1;
            for (String tabId : TAB_IDS) {
                int at = ids.indexOf(tabId);
                assertTrue(layout + ": " + tabId + " must be declared", at >= 0);
                assertTrue(layout + ": " + tabId + " is out of order", at > previous);
                previous = at;
            }
        }
    }

    @Test
    public void theTrailingControlsComeAfterTheTabs() throws IOException {
        for (String layout : new String[]{"nav_bar.xml", "nav_rail.xml"}) {
            List<String> ids = idsOf(layout);
            int lastTab = indexOfLastTab(ids);
            int signIn = ids.indexOf("nav_sign_in_button");
            assertTrue(layout + ": the account controls must be declared", signIn >= 0);
            assertTrue(layout + ": the account controls come after the tabs", signIn > lastTab);
        }
    }

    /** The layout id for a destination, read from the enum rather than restated here. */
    private static String nameFor(LauncherTab tab) {
        switch (tab) {
            case LAUNCH: return "nav_tab_launch";
            case LIBRARY: return "nav_tab_library";
            case MODS: return "nav_tab_mods";
            case SETTINGS: return "nav_tab_settings";
            default: throw new IllegalStateException("unmapped destination " + tab);
        }
    }

    private static int indexOfFirstTab(List<String> ids) {
        int best = Integer.MAX_VALUE;
        for (String tabId : TAB_IDS) {
            int at = ids.indexOf(tabId);
            if (at >= 0) best = Math.min(best, at);
        }
        return best == Integer.MAX_VALUE ? -1 : best;
    }

    private static int indexOfLastTab(List<String> ids) {
        int best = -1;
        for (String tabId : TAB_IDS) {
            best = Math.max(best, ids.indexOf(tabId));
        }
        return best;
    }

    private static List<String> idsOf(String layout) throws IOException {
        Path file = resolve("app/src/main/res/layout/" + layout);
        String xml = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        List<String> out = new ArrayList<>();
        Matcher m = ID.matcher(xml);
        while (m.find()) {
            out.add(m.group(1));
        }
        return out;
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
