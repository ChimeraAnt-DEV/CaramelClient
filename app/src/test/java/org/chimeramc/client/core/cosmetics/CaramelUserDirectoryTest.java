package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.chimeramc.client.core.voice.VoicePeer;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * The Caramel-user badge rule, pinned without a device or a network.
 *
 * <p>A badge beside a name is a claim about another person: "this account runs CaramelClient". The
 * only honest evidence is that we heard the account on a Caramel-only protocol, so the rule is
 * exercised here — an advert's xuid confirms an account, and nothing else does.
 */
public class CaramelUserDirectoryTest {

    private static CosmeticSyncProtocol.Advert advert(String xuid) {
        return CosmeticSyncProtocol.decode(CosmeticSyncProtocol.encode(
                "peer-1", "Device", xuid, "cape_x", CosmeticCatalog.NONE, CosmeticCatalog.NONE));
    }

    @Test
    public void anAdvertsXuidConfirmsTheAccount() {
        Set<String> ids = CaramelUserDirectory.confirmedIds(
                Collections.singletonList(advert("2533274812345678")), null);
        assertTrue(ids.contains("2533274812345678"));
    }

    @Test
    public void anEmptyXuidConfirmsNothingByIdentity() {
        // A version-1 peer (no xuid) is still a Caramel user, but only by its weak ids, never by an
        // empty string that could accidentally match a half-resolved account.
        Set<String> ids = CaramelUserDirectory.confirmedIds(
                Collections.singletonList(advert("")), null);
        assertFalse(ids.contains(""));
        assertTrue(ids.contains("peer-1"));
    }

    @Test
    public void aVoicePeerConfirmsByDeviceId() {
        VoicePeer peer = new VoicePeer("id-1", "Alex", 0f, 0f, 0f, "open", 1000L);
        Set<String> ids = CaramelUserDirectory.confirmedIds(null, Collections.singletonList(peer));
        assertTrue(ids.contains("alex"));
        assertTrue(ids.contains("id-1"));
    }

    @Test
    public void idsAreLowerCasedSoMatchingIsCaseInsensitive() {
        List<CosmeticSyncProtocol.Advert> adverts =
                Collections.singletonList(advert("ABCDEF1234567890"));
        Set<String> ids = CaramelUserDirectory.confirmedIds(adverts, null);
        assertTrue(ids.contains("abcdef1234567890"));
        assertEquals(3, ids.size()); // the xuid, the peer id, and the display name
    }

    @Test
    public void noPeersIsAnEmptyDirectory() {
        assertTrue(CaramelUserDirectory.confirmedIds(null, null).isEmpty());
        assertTrue(CaramelUserDirectory.confirmedIds(
                Collections.emptyList(), Collections.emptyList()).isEmpty());
        // A null entry must not throw.
        assertTrue(CaramelUserDirectory.confirmedIds(
                Arrays.asList((CosmeticSyncProtocol.Advert) null),
                Arrays.asList((VoicePeer) null)).isEmpty());
    }

    @Test
    public void aPublishedIdEarnsTheBadgeWithoutHavingMetTheAccount() {
        // The gap the friends screen had: a Caramel user you have never stood next to. The published
        // list is the evidence, and it is the only thing that makes this id a Caramel user here.
        Set<String> heard = Collections.emptySet();
        Set<String> published = Collections.singleton("2533274812345678");
        assertTrue(CaramelUserDirectory.isCaramelUser("2533274812345678", heard, published));
        // ...and the wire set keeps working on its own.
        assertTrue(CaramelUserDirectory.isCaramelUser(
                "2533274811111111", Collections.singleton("2533274811111111"),
                Collections.emptySet()));
    }

    @Test
    public void anUnknownIdIsStillNotABadge() {
        Set<String> published = Collections.singleton("2533274812345678");
        assertFalse(CaramelUserDirectory.isCaramelUser("2533274899999999",
                Collections.emptySet(), published));
        assertFalse("a blank id must never match", CaramelUserDirectory.isCaramelUser(
                "  ", Collections.emptySet(), published));
        assertFalse(CaramelUserDirectory.isCaramelUser(null, null, published));
        assertFalse(CaramelUserDirectory.isCaramelUser("2533274812345678", null, null));
    }

    @Test
    public void matchingIsCaseAndWhitespaceInsensitive() {
        assertTrue(CaramelUserDirectory.isCaramelUser(" 2533274812345678 ",
                Collections.emptySet(), Collections.singleton("2533274812345678")));
        assertTrue(CaramelUserDirectory.isCaramelUser("25332748ABCDEF01",
                Collections.emptySet(), Collections.singleton("25332748abcdef01")));
    }
}
