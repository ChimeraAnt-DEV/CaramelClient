package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The HUD readout is one shared glass panel, not a black rectangle.
 *
 * <p>A build machine cannot rasterise a layout, so the things that would silently regress are
 * asserted against the real files: the FPS readout uses the shared HUD panel (not the flat black
 * scrim the client's own users called the worst surface in the app), the level is carried by an
 * accent bar rather than by colouring the number, and the readouts keep the {@code stats_text} id the
 * overlays bind by name.
 */
public class HudReadoutLayoutTest {

    @Test
    public void theFpsReadoutUsesTheSharedGlassPanelNotTheFlatScrim() throws IOException {
        String hud = read("app/src/main/res/layout/overlay_hud_readout.xml");
        assertTrue("the FPS readout must use the shared glass HUD panel",
                hud.contains("@drawable/overlay_hud_panel"));
        assertFalse("the FPS readout must not go back to the flat black scrim",
                hud.contains("@color/overlay_scrim"));
    }

    @Test
    public void theSharedTextReadoutAlsoUsesTheGlassPanel() throws IOException {
        String readout = read("app/src/main/res/layout/overlay_stats_display.xml");
        assertTrue("the shared readout must use the glass HUD panel",
                readout.contains("@drawable/overlay_hud_panel"));
        assertTrue("the overlays find their TextView by this id",
                readout.contains("@+id/stats_text"));
    }

    @Test
    public void theLevelIsCarriedByAnAccentBarNotByTheNumbersColour() throws IOException {
        String hud = read("app/src/main/res/layout/overlay_hud_readout.xml");
        assertTrue("the readout must carry a level accent bar", hud.contains("@+id/hud_accent"));
        // The number's colour is the high-contrast overlay text, fixed; the level is the bar's
        // colour and the shape cue on the stats line. A coloured number is hard to read on a
        // bright scene and says nothing to a colour-blind player.
        String numberBlock = hud.substring(hud.indexOf("@+id/stats_text"),
                hud.indexOf("@+id/hud_unit"));
        assertTrue("the number must keep the fixed high-contrast colour",
                numberBlock.contains("@color/overlay_text_primary"));
    }

    @Test
    public void theFpsOverlayBindsTheHudPanelIds() throws IOException {
        String source = read("app/src/main/java/org/chimeramc/client/launcher/core/mods/inbuilt/overlay/FpsDisplayOverlay.java");
        assertTrue("the overlay must inflate the HUD panel",
                source.contains("R.layout.overlay_hud_readout"));
        assertTrue(source.contains("R.id.stats_text"));
        assertTrue(source.contains("R.id.hud_accent"));
        assertTrue(source.contains("R.id.hud_stats"));
        // The unit label is static, so it lives in the layout rather than costing a lookup.
        assertTrue("the unit label must be declared in the layout",
                read("app/src/main/res/layout/overlay_hud_readout.xml").contains("@+id/hud_unit"));
        assertFalse("the retired plain layout must be gone",
                source.contains("R.layout.overlay_stats_display"));
    }

    @Test
    public void theGraphIsAChildOfTheOneLayoutRatherThanASecondLayout() throws IOException {
        String hud = read("app/src/main/res/layout/overlay_hud_readout.xml");
        assertTrue("the graph is a child of the HUD panel", hud.contains("@+id/frame_time_graph"));
        assertFalse("the second, hand-kept graph layout is gone",
                Files.exists(root().resolve("app/src/main/res/layout/overlay_stats_display_graph.xml")));
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
