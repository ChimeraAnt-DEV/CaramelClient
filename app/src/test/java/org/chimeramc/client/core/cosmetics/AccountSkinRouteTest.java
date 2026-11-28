package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * A skin lookup says which route it took.
 *
 * <p>Two routes can produce an account skin and they fail for different reasons: the official
 * Minecraft Services profile needs a bearer (and a Java profile behind it), the legacy GeyserMC
 * lookup only knows players a GeyserMC server has seen. A bare "no skin" line cannot be acted on, so
 * the route is a value rather than a comment a reader has to reconstruct from the code.
 */
public class AccountSkinRouteTest {

    @Test
    public void aProfileSkinIsTheOfficialRoute() {
        assertEquals(AccountSkinService.SkinRoute.OFFICIAL,
                AccountSkinService.routeFor(true, true));
    }

    @Test
    public void noBearerIsNotTheSameAsABedrockOnlyProfile() {
        assertEquals("nothing to mint: the sign-in did not produce a services token",
                AccountSkinService.SkinRoute.LEGACY_WITHOUT_BEARER,
                AccountSkinService.routeFor(false, false));
        assertEquals("a bearer that produced no skin: this account has no Java profile skin",
                AccountSkinService.SkinRoute.LEGACY_AFTER_OFFICIAL_EMPTY,
                AccountSkinService.routeFor(false, true));
    }
}
