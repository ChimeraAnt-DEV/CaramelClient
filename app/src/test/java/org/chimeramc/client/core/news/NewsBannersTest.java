package org.chimeramc.client.core.news;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.chimeramc.client.R;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Which picture a news post draws, and that each picture ships.
 *
 * <p>The four banners are published art rather than vectors now, so the two ways this can break are
 * pinned here: a post asking for a banner the client does not know (which must fall back to its
 * severity, never to nothing), and a drawable name that no file backs (which would draw an empty
 * frame on the card until someone opened that tab on a device).
 */
public class NewsBannersTest {

    private static final Path ROOT = findRoot();

    @Test
    public void severityPicksTheBannerWhenThePostNamesNone() {
        assertEquals(R.drawable.news_banner_emergency,
                NewsBanners.drawableFor("", NewsSeverity.EMERGENCY));
        assertEquals(R.drawable.news_banner_urgent,
                NewsBanners.drawableFor(null, NewsSeverity.URGENT));
        // A published post that carries no severity reads as urgent, and must still get art.
        assertEquals(R.drawable.news_banner_urgent,
                NewsBanners.drawableFor("", NewsSeverity.fromWire("")));
    }

    @Test
    public void anExplicitKeyWinsOverTheSeverity() {
        assertEquals(R.drawable.news_banner_christmas,
                NewsBanners.drawableFor("christmas", NewsSeverity.URGENT));
        assertEquals(R.drawable.news_banner_plus,
                NewsBanners.drawableFor("caramel_plus", NewsSeverity.EMERGENCY));
        assertEquals(R.drawable.news_banner_plus,
                NewsBanners.drawableFor("Caramel+", NewsSeverity.URGENT));
        assertEquals(R.drawable.news_banner_emergency,
                NewsBanners.drawableFor("  EMERGENCY ", NewsSeverity.URGENT));
    }

    @Test
    public void anUnknownKeyFallsBackToTheSeverityRatherThanToNothing() {
        assertNotEquals(0, NewsBanners.drawableFor("sparkles", NewsSeverity.EMERGENCY));
        assertEquals(R.drawable.news_banner_emergency,
                NewsBanners.drawableFor("sparkles", NewsSeverity.EMERGENCY));
        assertEquals(R.drawable.news_banner_urgent,
                NewsBanners.drawableFor("sparkles", NewsSeverity.URGENT));
    }

    @Test
    public void aRowWithNeitherKeyNorSeverityKeepsTheGradient() {
        assertEquals(NewsBanners.NO_BANNER, NewsBanners.drawableFor("", null));
        assertEquals(NewsBanners.NO_BANNER, NewsBanners.drawableFor("sparkles", null));
        assertEquals(R.drawable.news_banner_berryapi, NewsBanners.drawableFor("berryapi", null));
    }

    @Test
    public void everyBannerTheMappingNamesShips() throws IOException {
        assertBannerFile("news_banner_emergency", "webp");
        assertBannerFile("news_banner_urgent", "webp");
        assertBannerFile("news_banner_christmas", "webp");
        assertBannerFile("news_banner_plus", "webp");
        assertBannerFile("news_banner_beta10", "webp");
    }

    @Test
    public void theReleaseCardNamesItsOwnBanner() throws IOException {
        // The newest section is the card the News tab opens on, so it has to name the art that was
        // made for it rather than falling back to the gradient.
        assertEquals(R.drawable.news_banner_beta10,
                NewsBanners.drawableFor(NewsBanners.KEY_BETA10, null));
        String feed = read("resources/launcher/changelog_feed.json");
        assertTrue("the shipped feed must give its newest section a banner",
                feed.contains("\"banner\": \"" + NewsBanners.KEY_BETA10 + "\""));
    }

    @Test
    public void theBundledChristmasAndPlusPostsDrawTheNewArt() throws IOException {
        String bundled = read("app/src/main/java/org/chimeramc/client/core/changelog/BundledNews.java");
        assertTrue("the Christmas post must draw the Christmas banner",
                bundled.contains("R.drawable.news_banner_christmas)"));
        assertTrue("the Caramel+ post must draw the Caramel+ banner",
                bundled.contains("R.drawable.news_banner_plus)"));
    }

    @Test
    public void theBannerFieldIsDocumentedForPublishers() throws IOException {
        String doc = read("resources/launcher/emergency_news.json");
        assertTrue("publishers need the banner key documented where they edit the document",
                doc.contains("banner is optional"));
    }

    private static void assertBannerFile(String name, String extension) throws IOException {
        Path png = ROOT.resolve("app/src/main/res/drawable-nodpi/" + name + "." + extension);
        assertTrue(name + " has no drawable in drawable-nodpi", Files.isRegularFile(png));
        assertTrue(name + " is suspiciously small to be art", Files.size(png) > 5_000L);
        // The placeholder vector under drawable/ would shadow or duplicate the name.
        Path xml = ROOT.resolve("app/src/main/res/drawable/" + name + ".xml");
        assertTrue(name + " still has a vector placeholder next to the art", Files.notExists(xml));
    }

    private static String read(String relative) throws IOException {
        return new String(Files.readAllBytes(ROOT.resolve(relative)), StandardCharsets.UTF_8);
    }

    private static Path findRoot() {
        Path dir = Paths.get("").toAbsolutePath();
        for (Path p = dir; p != null; p = p.getParent()) {
            if (Files.isRegularFile(p.resolve("settings.gradle"))) return p;
        }
        return dir;
    }
}
