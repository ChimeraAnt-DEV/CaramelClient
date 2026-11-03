package org.chimeramc.client.launcher.core.content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

/**
 * The pure share rules. No mocks: the naming and typing are decided from a real
 * {@link File}, so the test uses a real temp file and a real temp directory.
 */
public class ScreenshotSharingTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void aPngIsDeclaredAsPng() throws Exception {
        File png = folder.newFile("glowberry_1.png");
        assertEquals("image/png", ScreenshotSharing.mimeType(png));
    }

    @Test
    public void aJpegIsDeclaredAsJpegNotPng() throws Exception {
        // A receiver that trusts the declared type renders a JPEG declared as PNG as garbage.
        assertEquals("image/jpeg", ScreenshotSharing.mimeType(folder.newFile("shot.JPG")));
        assertEquals("image/jpeg", ScreenshotSharing.mimeType(folder.newFile("shot.jpeg")));
    }

    @Test
    public void anUnknownExtensionFallsBackToPngNotAWildcard() throws Exception {
        assertEquals("image/png", ScreenshotSharing.mimeType(folder.newFile("shot.bin")));
        assertEquals("image/png", ScreenshotSharing.mimeType(null));
    }

    @Test
    public void theTitleDropsTheExtension() throws Exception {
        assertEquals("Screenshot_2026-10-30-14-03-11",
                ScreenshotSharing.shareTitle(folder.newFile("Screenshot_2026-10-30-14-03-11.png")));
    }

    @Test
    public void aDotfileKeepsItsWholeName() throws Exception {
        // A leading dot is not an extension separator; stripping it would leave an empty title.
        assertEquals(".hidden", ScreenshotSharing.shareTitle(folder.newFile(".hidden")));
    }

    @Test
    public void anEmptyOrNullFileFallsBackToAStableTitle() {
        assertEquals(ScreenshotSharing.FALLBACK_TITLE, ScreenshotSharing.shareTitle(null));
    }

    @Test
    public void onlyARealRegularFileIsShareable() throws Exception {
        assertTrue(ScreenshotSharing.isShareable(folder.newFile("real.png")));
        assertFalse(ScreenshotSharing.isShareable(null));
        assertFalse("a directory is not shareable",
                ScreenshotSharing.isShareable(folder.newFolder("Screenshots")));
        assertFalse("a path that does not exist is not shareable",
                ScreenshotSharing.isShareable(new File(folder.getRoot(), "missing.png")));
    }

    @Test
    public void extensionIsLowerCasedWithoutTheDot() {
        assertEquals("png", ScreenshotSharing.extension("a.PNG"));
        assertEquals("", ScreenshotSharing.extension("noextension"));
        assertEquals("", ScreenshotSharing.extension("trailing."));
        assertEquals("", ScreenshotSharing.extension(null));
    }

    @Test
    public void anImageNameIsRecognisedByItsExtension() {
        assertTrue(ScreenshotStore.isImage("a.png"));
        assertTrue(ScreenshotStore.isImage("A.JPEG"));
        assertFalse(ScreenshotStore.isImage("world.mcworld"));
        assertFalse(ScreenshotStore.isImage(null));
    }
}
