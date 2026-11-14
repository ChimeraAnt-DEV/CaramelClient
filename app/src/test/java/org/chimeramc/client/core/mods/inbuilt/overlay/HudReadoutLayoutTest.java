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
        // The overlay reads the panel through the generated ViewBinding, so a renamed id is a
        // compile error rather than a null at runtime. The binding is generated from the layout, so
        // the layout still owns the ids.
        assertTrue("the overlay must inflate the HUD panel through its ViewBinding",
                source.contains("OverlayHudReadoutBinding.inflate("));
        assertTrue(source.contains("binding.statsText"));
        assertTrue(source.contains("binding.hudAccent"));
        assertTrue(source.contains("binding.hudStats"));
        String layout = read("app/src/main/res/layout/overlay_hud_readout.xml");
        assertTrue(layout.contains("@+id/stats_text"));
        assertTrue(layout.contains("@+id/hud_accent"));
        assertTrue(layout.contains("@+id/hud_stats"));
        // The unit label is static, so it lives in the layout rather than costing a lookup.
        assertTrue("the unit label must be declared in the layout",
                layout.contains("@+id/hud_unit"));
        assertFalse("the retired plain layout must be gone",
                source.contains("R.layout.overlay_stats_display"));
    }

    @Test
    public void theReadoutMarksTheLauncherRefreshRateAsAnApproximation() throws IOException {
        String hud = read("app/src/main/res/layout/overlay_hud_readout.xml");
        // The launcher's own refresh cadence is a different measurement from the game's frame rate;
        // the panel carries a marker element so the number is never passed off as the game's FPS.
        assertTrue("the readout must carry the approximation marker",
                hud.contains("@+id/hud_approx"));
        String source = read("app/src/main/java/org/chimeramc/client/launcher/core/mods/inbuilt/overlay/FpsDisplayOverlay.java");
        assertTrue("the overlay must choose the reading through FpsReadout",
                source.contains("FpsReadout.of("));
    }

    @Test
    public void theGraphIsAChildOfTheOneLayoutRatherThanASecondLayout() throws IOException {
        String hud = read("app/src/main/res/layout/overlay_hud_readout.xml");
        assertTrue("the graph is a child of the HUD panel", hud.contains("@+id/frame_time_graph"));
        assertFalse("the second, hand-kept graph layout is gone",
                Files.exists(root().resolve("app/src/main/res/layout/overlay_stats_display_graph.xml")));
    }

    /**
     * The host meter counts the launcher's own {@code Choreographer} callbacks, which is the
     * launcher main thread's vsync tick — not the game's frame rate and not the game's present rate.
     * Every surface that shows it must name it as the launcher refresh; the strings and the marker
     * element are asserted here so a re-description cannot creep back in.
     */
    @Test
    public void theHostMeterIsNamedAsTheLauncherRefreshNotTheGameRate() throws IOException {
        String meter = read("app/src/main/java/org/chimeramc/client/launcher/core/mods/inbuilt/overlay/FrameRateMeter.java");
        assertTrue("the meter's own doc must say it is not the game's rate",
                meter.contains("not</em> the game's frame rate"));
        assertFalse("the meter must not be described as the game's present rate",
                meter.contains("the rate the game's"));
        String strings = read("app/src/main/res/values/strings.xml");
        assertTrue("the marker string must name the launcher refresh",
                strings.contains("name=\"mod_overlay_fps_launcher_note\""));
        assertFalse("the retired present-rate string must be gone",
                strings.contains("mod_overlay_fps_approx_note"));
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
