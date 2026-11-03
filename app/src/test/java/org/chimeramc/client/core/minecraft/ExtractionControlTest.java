package org.chimeramc.client.core.minecraft;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * A cancelled extraction must stop at the next safe boundary.
 */
public class ExtractionControlTest {

    @Test
    public void aFreshControlIsNotCancelled() {
        assertFalse(new ExtractionControl().isCancelled());
    }

    @Test
    public void cancelFlipsTheFlag() {
        ExtractionControl control = new ExtractionControl();
        control.cancel();
        assertTrue(control.isCancelled());
    }

    @Test
    public void cancelIsIdempotent() {
        ExtractionControl control = new ExtractionControl();
        control.cancel();
        control.cancel();
        assertTrue(control.isCancelled());
    }

    @Test
    public void throwIfCancelledIsSilentBeforeACancel() throws Exception {
        new ExtractionControl().throwIfCancelled();
    }

    @Test
    public void throwIfCancelledThrowsAfterACancel() {
        ExtractionControl control = new ExtractionControl();
        control.cancel();
        try {
            control.throwIfCancelled();
            fail("a cancelled extraction must stop");
        } catch (ExtractionControl.ExtractionCancelledException expected) {
            assertTrue(expected.getMessage().contains("cancelled"));
        }
    }

    /** The flag is read from another thread, so the write must be visible without a lock. */
    @Test
    public void theSignalCrossesThreads() throws Exception {
        ExtractionControl control = new ExtractionControl();
        Thread worker = new Thread(control::cancel);
        worker.start();
        worker.join(2000);

        assertTrue("a cancel from another thread must be visible", control.isCancelled());
    }
}
