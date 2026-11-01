package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Hotbar slot state de-duplication (item 196).
 *
 * <p>Pins the rule that a repeated state is not pushed to the backend, and that any change to any
 * field is.
 */
public class HotbarSlotSyncTest {

    private static final String STATE =
            HotbarSlotSync.key(10, 20, 100, 100, 1920, 1080, 1f, true, false);

    @Test
    public void theFirstPushIsAlwaysSent() {
        HotbarSlotSync sync = new HotbarSlotSync();
        assertTrue(sync.isEmpty());
        assertTrue(sync.shouldPush(STATE));
        assertFalse(sync.isEmpty());
    }

    @Test
    public void aRepeatedStateIsNotPushed() {
        HotbarSlotSync sync = new HotbarSlotSync();
        assertTrue(sync.shouldPush(STATE));
        assertFalse(sync.shouldPush(STATE));
        assertFalse(sync.shouldPush(STATE));
    }

    @Test
    public void aChangedFieldIsPushed() {
        HotbarSlotSync sync = new HotbarSlotSync();
        sync.shouldPush(STATE);
        assertTrue(sync.shouldPush(
                HotbarSlotSync.key(10, 20, 100, 100, 1920, 1080, 1f, true, true)));
        assertTrue(sync.shouldPush(
                HotbarSlotSync.key(11, 20, 100, 100, 1920, 1080, 1f, true, false)));
    }

    @Test
    public void aNullKeyIsRefused() {
        HotbarSlotSync sync = new HotbarSlotSync();
        assertFalse(sync.shouldPush(null));
        assertTrue(sync.isEmpty());
    }

    @Test
    public void resetMakesTheNextPushSendAgain() {
        HotbarSlotSync sync = new HotbarSlotSync();
        sync.shouldPush(STATE);
        assertFalse(sync.shouldPush(STATE));
        sync.reset();
        assertTrue(sync.isEmpty());
        assertTrue(sync.shouldPush(STATE));
    }

    @Test
    public void theFingerprintDistinguishesEveryField() {
        assertNotEquals(HotbarSlotSync.key(0, 0, 1, 1, 2, 2, 1f, true, false),
                HotbarSlotSync.key(1, 0, 1, 1, 2, 2, 1f, true, false));
        assertNotEquals(HotbarSlotSync.key(0, 0, 1, 1, 2, 2, 1f, true, false),
                HotbarSlotSync.key(0, 0, 1, 1, 2, 2, 0.5f, true, false));
        assertNotEquals(HotbarSlotSync.key(0, 0, 1, 1, 2, 2, 1f, true, false),
                HotbarSlotSync.key(0, 0, 1, 1, 2, 2, 1f, false, false));
        assertEquals(HotbarSlotSync.key(0, 0, 1, 1, 2, 2, 1f, true, false),
                HotbarSlotSync.key(0, 0, 1, 1, 2, 2, 1f, true, false));
    }
}
