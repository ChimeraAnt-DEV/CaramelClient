package org.chimeramc.client.ui.components;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Every shared button surface has a pressed state.
 *
 * <p>A button that looks the same whether or not it is being touched is the "animated UI buttons look
 * trash" report: it feels dead. The fix has two halves — the code-side geometry (pinned by
 * {@code UiButtonPressTest}) and the XML drawables, which must actually declare a pressed look rather
 * than a single flat shape. These read the real drawables, because a build machine cannot tap one.
 */
public class ButtonPressDrawableTest {

    @Test
    public void theAccentButtonDeclaresAPressedFace() throws IOException {
        String xml = read("app/src/main/res/drawable/button_accent.xml");
        assertTrue("the accent button must be a ripple, so a touch animates",
                xml.contains("<ripple"));
        assertTrue("it must declare a pressed state", xml.contains("android:state_pressed=\"true\""));
        assertTrue("the pressed face must be lighter than the resting one",
                xml.contains("ui_button_press_overlay"));
    }

    @Test
    public void thePlaySlabDepressesOnPress() throws IOException {
        String xml = read("app/src/main/res/drawable/bg_dock_play.xml");
        assertTrue("the Play slab must be a ripple", xml.contains("<ripple"));
        assertTrue("it must declare a pressed state", xml.contains("android:state_pressed=\"true\""));
        // The pressed face is the resting face shifted down by the edge depth, so the slab sinks.
        assertTrue("the pressed face must be shifted down by the edge depth",
                xml.contains("android:top=\"3dp\""));
        // The resting face still has the darker edge proud at the bottom.
        assertTrue("the resting slab must keep its raised edge", xml.contains("android:bottom=\"3dp\""));
    }

    @Test
    public void theInstanceChipDeclaresAPressedFace() throws IOException {
        String xml = read("app/src/main/res/drawable/bg_dock_version_chip.xml");
        assertTrue("the instance chip must be a ripple", xml.contains("<ripple"));
        assertTrue("it must declare a pressed state", xml.contains("android:state_pressed=\"true\""));
        assertTrue("the pressed stone must be a named colour, not a literal",
                xml.contains("@color/mc_stone_pressed"));
    }

    @Test
    public void everyButtonDeclaresAVisibleFocusState() throws IOException {
        // The app is driven by a controller and a keyboard as well as touch, so a focusable button
        // with no visible focus state leaves a controller user with no idea what is selected. Every
        // shared button drawable must declare a focused look.
        String[] buttons = {
                "app/src/main/res/drawable/button_accent.xml",
                "app/src/main/res/drawable/bg_dock_play.xml",
                "app/src/main/res/drawable/bg_dock_version_chip.xml",
        };
        for (String path : buttons) {
            String xml = read(path);
            assertTrue(path + " must declare a focused state, or a controller user cannot see "
                    + "what is selected", xml.contains("android:state_focused=\"true\""));
        }
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(root().resolve(relativePath)), StandardCharsets.UTF_8);
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
