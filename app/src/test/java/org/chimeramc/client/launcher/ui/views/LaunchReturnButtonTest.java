package org.chimeramc.client.launcher.ui.views;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The launch screen's return button carries a label and pops in.
 *
 * <p>On a failed launch the button was revealed with a bare {@code VISIBLE} and had no
 * {@code android:text}, so the user saw an empty pill where "Back" should be. The button now has a
 * label and a pop-up entrance; this pins both so the label cannot be dropped again and the reveal
 * keeps its animation.
 */
public class LaunchReturnButtonTest {

    @Test
    public void theReturnButtonHasALabel() throws IOException {
        String xml = read("app/src/main/res/layout/activity_minecraft_loading.xml");
        int idAt = xml.indexOf("@+id/minecraftLaunchReturn");
        assertTrue("the return button must exist", idAt > 0);
        int openAt = xml.lastIndexOf("<", idAt);
        int closeAt = xml.indexOf("/>", idAt);
        String block = xml.substring(openAt, closeAt);
        assertTrue("the return button must set a label or it renders blank. attrs: " + block,
                block.contains("android:text=\"@string/back\""));
    }

    @Test
    public void theRevealPopsTheButtonIn() throws IOException {
        String kt = read("app/src/main/java/org/chimeramc/client/launcher/core/minecraft/"
                + "MinecraftLoadingActivity.kt");
        assertTrue("the reveal must be a helper, not a bare VISIBLE", kt.contains("showReturnButton()"));
        int at = kt.indexOf("private fun showReturnButton()");
        assertTrue("the helper must exist", at > 0);
        String body = kt.substring(at, kt.indexOf('}', at));
        assertTrue("the helper must label the button", body.contains("setText(R.string.back)"));
        assertTrue("the helper must pop the button in",
                body.contains("DynamicAnim.overshootScaleIn(returnButton"));
        assertFalse("the raw VISIBLE reveal must be gone",
                kt.contains("recordCrash(this)\n        returnButton.visibility = View.VISIBLE"));
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
