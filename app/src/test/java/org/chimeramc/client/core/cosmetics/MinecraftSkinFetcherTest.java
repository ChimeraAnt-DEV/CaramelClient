package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * The Minecraft Services profile parser.
 *
 * <p>This is the replacement for the third-party lookup that returned nothing for a player it had
 * never seen. The shape of the response is pinned here so a change is caught by a test rather than
 * by a player seeing the wrong skin.
 */
public class MinecraftSkinFetcherTest {

    /** A realistic profile: an inactive skin first, the worn one second. */
    private static final String TWO_SKINS = "{"
            + "\"id\":\"069a79f444e94726a5befca90e38aaf5\","
            + "\"name\":\"Notch\","
            + "\"skins\":["
            + "  {\"id\":\"a1\",\"state\":\"INACTIVE\","
            + "   \"url\":\"http://textures.minecraft.net/texture/inactive\","
            + "   \"variant\":\"CLASSIC\"},"
            + "  {\"id\":\"a2\",\"state\":\"ACTIVE\","
            + "   \"url\":\"http://textures.minecraft.net/texture/active\","
            + "   \"variant\":\"SLIM\"}"
            + "]}";

    @Test
    public void theActiveSkinIsPreferred() {
        assertEquals("http://textures.minecraft.net/texture/active",
                MinecraftSkinFetcher.activeSkinUrl(TWO_SKINS));
    }

    @Test
    public void aSingleSkinIsUsedEvenWithoutAnActiveMarker() {
        // Common for a new account. Refusing it would send the caller to the fallback for no reason.
        String body = "{\"id\":\"x\",\"name\":\"n\",\"skins\":["
                + "{\"id\":\"a1\",\"url\":\"http://textures.minecraft.net/texture/only\"}]}";
        assertEquals("http://textures.minecraft.net/texture/only",
                MinecraftSkinFetcher.activeSkinUrl(body));
    }

    @Test
    public void theFirstUrlWinsWhenNothingIsMarkedActive() {
        String body = "{\"skins\":["
                + "{\"id\":\"a1\",\"url\":\"http://t/first\"},"
                + "{\"id\":\"a2\",\"url\":\"http://t/second\"}]}";
        assertEquals("http://t/first", MinecraftSkinFetcher.activeSkinUrl(body));
    }

    @Test
    public void anActiveEntryWithNoUrlFallsBackToAnotherEntry() {
        String body = "{\"skins\":["
                + "{\"id\":\"a1\",\"state\":\"ACTIVE\"},"
                + "{\"id\":\"a2\",\"state\":\"INACTIVE\",\"url\":\"http://t/other\"}]}";
        assertEquals("http://t/other", MinecraftSkinFetcher.activeSkinUrl(body));
    }

    @Test
    public void anEmptySkinsArrayIsNoSkin() {
        assertNull(MinecraftSkinFetcher.activeSkinUrl("{\"id\":\"x\",\"skins\":[]}"));
    }

    @Test
    public void aMissingSkinsFieldIsNoSkin() {
        assertNull(MinecraftSkinFetcher.activeSkinUrl("{\"id\":\"x\",\"name\":\"n\"}"));
    }

    @Test
    public void aNonObjectOrMalformedBodyIsNoSkinNotACrash() {
        assertNull(MinecraftSkinFetcher.activeSkinUrl(null));
        assertNull(MinecraftSkinFetcher.activeSkinUrl(""));
        assertNull(MinecraftSkinFetcher.activeSkinUrl("   "));
        assertNull(MinecraftSkinFetcher.activeSkinUrl("not json at all"));
        assertNull(MinecraftSkinFetcher.activeSkinUrl("[1,2,3]"));
        assertNull(MinecraftSkinFetcher.activeSkinUrl("<html>401</html>"));
    }

    @Test
    public void aSkinsFieldThatIsNotAnArrayIsNoSkin() {
        assertNull(MinecraftSkinFetcher.activeSkinUrl("{\"skins\":\"nope\"}"));
        assertNull(MinecraftSkinFetcher.activeSkinUrl("{\"skins\":null}"));
    }

    @Test
    public void entriesThatAreNotObjectsAreSkipped() {
        String body = "{\"skins\":[null,42,{\"url\":\"http://t/real\"}]}";
        assertEquals("http://t/real", MinecraftSkinFetcher.activeSkinUrl(body));
    }

    @Test
    public void theProfileIdIsReadWhenPresent() {
        assertEquals("069a79f444e94726a5befca90e38aaf5",
                MinecraftSkinFetcher.profileId(TWO_SKINS));
        assertNull(MinecraftSkinFetcher.profileId("{\"name\":\"n\"}"));
        assertNull(MinecraftSkinFetcher.profileId("not json"));
        assertNull(MinecraftSkinFetcher.profileId(null));
    }

    @Test
    public void theEndpointIsTheOfficialProfileUrl() {
        // Pinned so a typo cannot silently point the lookup at the wrong service.
        assertEquals("https://api.minecraftservices.com/minecraft/profile",
                MinecraftSkinFetcher.PROFILE_URL);
    }
}
