package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * The skin lookup must work for Bedrock-only players.
 *
 * <p><b>Why this test exists.</b> The client's users own Bedrock, not Java Edition. If the path that
 * mints the Minecraft Services bearer enforced Java ownership (a {@code game_minecraft} entitlement),
 * every one of them would fail to get a skin. The bearer only needs the XSTS exchange against
 * {@code rp://api.minecraftservices.com/} and the {@code /launcher/login} call; entitlements are a
 * separate, lazily-fetched endpoint that this path must never touch.
 *
 * <p>These checks are deliberately two-layered: a payload test proving a Bedrock-only entitlement
 * body still yields a skin URL, and a source test proving the entitlement endpoint is not on the
 * skin path at all (so a future refactor cannot quietly introduce the requirement).
 */
public class BedrockOnlySkinLookupTest {

    /**
     * An entitlements response for an account that owns Bedrock but NOT Java.
     *
     * <p>{@code game_minecraft} (the Java entitlement) is absent; {@code product_minecraft_bedrock}
     * is present. This is exactly what a Bedrock-only account sees.
     */
    private static final String BEDROCK_ONLY_ENTITLEMENTS = "{"
            + "\"items\":["
            + "  {\"name\":\"product_minecraft_bedrock\",\"source\":\"PURCHASE\"},"
            + "  {\"name\":\"product_minecraft\",\"source\":\"PURCHASE\"}"
            + "],"
            + "\"signature\":\"…\","
            + "\"keyId\":\"1\"}";

    /** The same account's profile, which does carry a skin. */
    private static final String BEDROCK_ONLY_PROFILE = "{"
            + "\"id\":\"069a79f444e94726a5befca90e38aaf5\","
            + "\"name\":\"BedrockPlayer\","
            + "\"skins\":[{\"id\":\"a1\",\"state\":\"ACTIVE\","
            + "  \"url\":\"http://textures.minecraft.net/texture/bedrockskin\"}]}";

    @Test
    public void aBedrockOnlyAccountStillResolvesItsSkinUrl() {
        // The entitlement body is irrelevant to the skin lookup, which is the whole point: parsing it
        // must not be a precondition for reading the profile.
        String url = MinecraftSkinFetcher.activeSkinUrl(BEDROCK_ONLY_PROFILE);
        assertEquals("http://textures.minecraft.net/texture/bedrockskin", url);
    }

    @Test
    public void theBedrockEntitlementBodyIsNotMistakenForAProfile() {
        // If the entitlement response were ever fed to the profile parser, it must read as "no skin"
        // rather than as a crash or a wrong URL -- it has no skins array.
        assertNull(MinecraftSkinFetcher.activeSkinUrl(BEDROCK_ONLY_ENTITLEMENTS));
    }

    @Test
    public void theSkinPathNeverReadsTheEntitlementsEndpoint() throws IOException {
        String service = read("app/src/main/java/org/chimeramc/client/core/cosmetics/"
                + "AccountSkinService.java");
        String auth = read("app/src/main/java/org/chimeramc/client/launcher/core/auth/"
                + "MsftAuthManager.java");
        for (String source : new String[] {service, auth}) {
            assertTrue("the skin path must not call the entitlements endpoint",
                    !source.contains("entitlements/mcstore"));
            assertTrue("the skin path must not request entitlements",
                    !source.contains("getMinecraftEntitlements"));
        }
    }

    @Test
    public void theBearerComesFromTheMinecraftRelyingParty() throws IOException {
        // The Java XSTS exchange targets rp://api.minecraftservices.com/. The library owns that
        // constant; what matters here is that we go through the Java chain's Minecraft token rather
        // than reusing the Bedrock token, whose XSTS is authorised for multiplayer.minecraft.net
        // and is therefore rejected by api.minecraftservices.com.
        String auth = read("app/src/main/java/org/chimeramc/client/launcher/core/auth/"
                + "MsftAuthManager.java");
        assertTrue("the bearer must be minted from the Java chain's Minecraft token",
                auth.contains("JavaAuthManager"));
        assertTrue("and it must use that token's authorization header",
                auth.contains("getMinecraftToken()"));
        assertTrue("and read the header the token exposes",
                auth.contains("getAuthorizationHeader()"));
    }

    private static String read(String relative) throws IOException {
        return new String(Files.readAllBytes(projectRoot().resolve(relative)),
                StandardCharsets.UTF_8);
    }

    /**
     * Walks up from the test working directory to the repository root.
     *
     * <p>Gradle runs unit tests from the module directory, not the repository root.
     */
    private static java.nio.file.Path projectRoot() {
        java.nio.file.Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            if (Files.exists(dir.resolve("app/src/main/res/layout"))) return dir;
            dir = dir.getParent();
        }
        throw new IllegalStateException("could not find the project root");
    }
}
