package org.chimeramc.client.launcher.core.mods.inbuilt.nativemod;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.chimeramc.client.core.backend.GameBackend;
import org.chimeramc.client.core.backend.GameBridge;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Every {@code nativemod} wrapper is a thin forwarder onto the active backend (item 353).
 *
 * <p>After the {@code org.levimc} strip these classes no longer hold native code of their own: each
 * one must route to {@link GameBridge} and, with no backend registered, do nothing at all - no
 * throw, no crash, no silent partial state. That is the property the launcher relies on to start a
 * game on a build where no script runtime is present.
 *
 * <p>The second half of the test registers a recording backend and checks the wrapper actually
 * reaches it, so a wrapper that quietly dropped its call (the failure mode of a half-finished
 * forwarder) is caught rather than passing as "does nothing when absent".
 */
public class ModWrapperRoutingTest {

    /** A backend that records what it was asked to do and supports everything by default. */
    private static final class RecordingBackend implements GameBackend {
        final List<String> calls = new ArrayList<>();
        private final java.util.Set<Feature> supported =
                java.util.EnumSet.allOf(Feature.class);

        @Override
        public int apiVersion() {
            return API_VERSION;
        }

        @Override
        public String name() {
            return "recording";
        }

        @Override
        public String version() {
            return "1";
        }

        @Override
        public boolean supports(Feature feature) {
            return supported.contains(feature);
        }

        @Override
        public boolean sendKey(int bedrockKeyCode, boolean down) {
            calls.add("sendKey(" + bedrockKeyCode + "," + down + ")");
            return true;
        }

        @Override
        public boolean sendMouseButton(int androidButton, boolean down) {
            calls.add("sendMouseButton(" + androidButton + "," + down + ")");
            return true;
        }

        @Override
        public boolean sendScroll(float vertical) {
            calls.add("sendScroll(" + vertical + ")");
            return true;
        }

        @Override
        public boolean sendLookDelta(float deltaX, float deltaY) {
            calls.add("sendLookDelta(" + deltaX + "," + deltaY + ")");
            return true;
        }

        @Override
        public boolean sendPointer(float x, float y) {
            calls.add("sendPointer(" + x + "," + y + ")");
            return true;
        }

        @Override
        public boolean setZoom(boolean active) {
            calls.add("setZoom(" + active + ")");
            return true;
        }

        @Override
        public boolean setAutoSprint(boolean enabled) {
            calls.add("setAutoSprint(" + enabled + ")");
            return true;
        }

        @Override
        public boolean isAutoSprintActive() {
            calls.add("isAutoSprintActive()");
            return true;
        }

        @Override
        public boolean selectHotbarSlot(int slot) {
            calls.add("selectHotbarSlot(" + slot + ")");
            return true;
        }

        @Override
        public int readFps() {
            calls.add("readFps()");
            return 60;
        }

        @Override
        public void releaseAll() {
            calls.add("releaseAll()");
        }
    }

    @Before
    public void setUp() {
        GameBridge.resetForTests();
    }

    @After
    public void tearDown() {
        GameBridge.resetForTests();
    }

    @Test
    public void everyWrapperIsInertWithNoBackend() {
        // Capability probes answer false rather than throwing.
        assertFalse(ZoomMod.init());
        assertFalse(SnaplookMod.init());
        assertFalse(FpsMod.init());
        assertFalse(FpsMod.nativeIsInitialized());
        assertFalse(AutoSprintMod.initialize());
        assertFalse(MoreButtonsMod.initialize());
        assertFalse(HotbarSlotMod.initialize());
        assertFalse(PojavControlsMod.initialize());
        assertFalse(GyroMod.init());
        assertFalse(GyroMod.nativeIsInitialized());

        // Input sends are no-ops, and none of them throw.
        ZoomMod.nativeOnKeyDown();
        ZoomMod.nativeOnKeyUp();
        ZoomMod.nativeOnScroll(1.5f);
        ZoomMod.nativeSetZoomLevel(4L);
        ZoomMod.nativeSetTransitionDuration(100);
        SnaplookMod.nativeOnKeyDown();
        SnaplookMod.nativeOnKeyUp();
        PojavControlsMod.setEnabled(true);
        PojavControlsMod.nativeSendKey(87, true);
        PojavControlsMod.nativeSendMouseButton(1, true);
        PojavControlsMod.nativeSendScroll(1f);
        PojavControlsMod.nativeSendLookDelta(1f, 2f);
        PojavControlsMod.nativeSendPointer(10f, 20f);
        MoreButtonsMod.sendKey(87, true);
        HotbarSlotMod.setEnabled(true);
        HotbarSlotMod.setSlotState(0, 0f, 0f, 10f, 10f, 100f, 100f, 1f, true, false);
        HotbarSlotMod.clearSlot(0);
        AutoSprintMod.setEnabled(true);
        AutoSprintMod.setEnabled(false);
        GyroMod.nativePreResolve();
        GyroMod.nativeSetEnabled(true);
        GyroMod.nativeUpdateDelta(1f, 1f);
        GyroMod.nativeSetSensitivityX(1f);
        GyroMod.nativeSetSensitivityY(1f);
        GyroMod.nativeSetInvertX(false);
        GyroMod.nativeSetInvertY(false);
        GyroMod.nativeSetDeadzone(0.1f);

        // An unknown reading is negative, never a fabricated number.
        assertTrue(FpsMod.nativeGetFps() < 0);
        assertFalse(HotbarSlotMod.hasItem(0));
    }

    @Test
    public void withABackendTheInputWrappersActuallyReachIt() {
        RecordingBackend backend = new RecordingBackend();
        GameBridge.register(backend);

        assertTrue(ZoomMod.init());
        assertTrue(FpsMod.init());
        assertTrue(PojavControlsMod.initialize());

        PojavControlsMod.nativeSendKey(87, true);
        PojavControlsMod.nativeSendMouseButton(1, false);
        PojavControlsMod.nativeSendScroll(2f);
        PojavControlsMod.nativeSendLookDelta(3f, 4f);
        PojavControlsMod.nativeSendPointer(5f, 6f);
        ZoomMod.nativeOnKeyDown();
        ZoomMod.nativeOnKeyUp();
        AutoSprintMod.setEnabled(true);

        assertTrue(backend.calls.toString(), backend.calls.contains("sendKey(87,true)"));
        assertTrue(backend.calls.toString(), backend.calls.contains("sendMouseButton(1,false)"));
        assertTrue(backend.calls.toString(), backend.calls.contains("sendScroll(2.0)"));
        assertTrue(backend.calls.toString(), backend.calls.contains("sendLookDelta(3.0,4.0)"));
        assertTrue(backend.calls.toString(), backend.calls.contains("sendPointer(5.0,6.0)"));
        assertTrue(backend.calls.toString(), backend.calls.contains("setZoom(true)"));
        assertTrue(backend.calls.toString(), backend.calls.contains("setZoom(false)"));
        assertTrue(backend.calls.toString(), backend.calls.contains("setAutoSprint(true)"));
    }

    @Test
    public void theFpsWrapperReportsTheBackendsReading() {
        GameBridge.register(new RecordingBackend());
        assertTrue(FpsMod.nativeGetFps() == 60);
    }
}
