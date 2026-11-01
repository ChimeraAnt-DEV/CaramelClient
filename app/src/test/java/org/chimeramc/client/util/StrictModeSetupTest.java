package org.chimeramc.client.util;

import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Pins the release no-op of {@link StrictModeSetup} (item 296).
 *
 * <p>The debug path calls {@code android.os.StrictMode}, which is a stub off-device, so it cannot be
 * exercised in a JVM test. What a unit test can prove is the property that matters for release: with
 * the flag off, {@code install} and {@code noteSlowCall} return before they touch any Android API, so
 * a release build never pays for a StrictMode check. If the early return were ever moved below a
 * call, these tests would throw {@code RuntimeException("Stub!")} and fail.
 */
public class StrictModeSetupTest {

    @Test
    public void installWithTheFlagOffDoesNotTouchStrictMode() {
        try {
            StrictModeSetup.install(false);
        } catch (RuntimeException e) {
            fail("install(false) reached a platform call: " + e);
        }
    }

    @Test
    public void noteSlowCallWithTheFlagOffDoesNotTouchStrictMode() {
        try {
            StrictModeSetup.noteSlowCall(false, "whatever");
        } catch (RuntimeException e) {
            fail("noteSlowCall(false) reached a platform call: " + e);
        }
    }
}
