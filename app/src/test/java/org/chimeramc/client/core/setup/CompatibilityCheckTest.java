package org.chimeramc.client.core.setup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

/**
 * The first-run compatibility checks (item 284). Pure: no device.
 */
public class CompatibilityCheckTest {

    private static CompatibilityCheck.Result byId(List<CompatibilityCheck.Result> results,
                                                  CompatibilityCheck.Id id) {
        for (CompatibilityCheck.Result r : results) {
            if (r.id == id) return r;
        }
        return null;
    }

    @Test
    public void aHealthyDevicePassesEveryCheck() {
        CompatibilityCheck.Facts facts = new CompatibilityCheck.Facts()
                .freeBytes(64L * 1024L * 1024L * 1024L)
                .abiCompatible(true)
                .playStoreInstall(false)
                .storagePermissionGranted(true);
        List<CompatibilityCheck.Result> results = CompatibilityCheck.evaluate(facts);
        assertEquals(4, results.size());
        assertTrue(CompatibilityCheck.allOk(results));
        assertFalse(CompatibilityCheck.hasBlocking(results));
    }

    @Test
    public void aFullVolumeBlocksWithAStorageFix() {
        CompatibilityCheck.Facts facts = new CompatibilityCheck.Facts()
                .freeBytes(512L * 1024L * 1024L);
        CompatibilityCheck.Result storage = byId(CompatibilityCheck.evaluate(facts),
                CompatibilityCheck.Id.FREE_STORAGE);
        assertEquals(CompatibilityCheck.Severity.BLOCKING, storage.severity);
        assertEquals(CompatibilityCheck.Fix.OPEN_STORAGE, storage.fix);
        assertTrue(CompatibilityCheck.hasBlocking(CompatibilityCheck.evaluate(facts)));
    }

    @Test
    public void aTightButUsableVolumeWarnsRatherThanBlocks() {
        CompatibilityCheck.Facts facts = new CompatibilityCheck.Facts()
                .freeBytes(3L * 1024L * 1024L * 1024L);
        CompatibilityCheck.Result storage = byId(CompatibilityCheck.evaluate(facts),
                CompatibilityCheck.Id.FREE_STORAGE);
        assertEquals(CompatibilityCheck.Severity.WARNING, storage.severity);
        assertFalse(storage.isBlocking());
    }

    @Test
    public void anUnknownFreeSpaceReadingWarnsRatherThanGuessing() {
        CompatibilityCheck.Facts facts = new CompatibilityCheck.Facts()
                .freeBytes(CompatibilityCheck.UNKNOWN_BYTES);
        CompatibilityCheck.Result storage = byId(CompatibilityCheck.evaluate(facts),
                CompatibilityCheck.Id.FREE_STORAGE);
        assertEquals(CompatibilityCheck.Severity.WARNING, storage.severity);
    }

    @Test
    public void anIncompatibleAbiBlocks() {
        CompatibilityCheck.Facts facts = new CompatibilityCheck.Facts()
                .freeBytes(64L * 1024L * 1024L * 1024L)
                .abiCompatible(false);
        CompatibilityCheck.Result abi = byId(CompatibilityCheck.evaluate(facts),
                CompatibilityCheck.Id.ABI);
        assertEquals(CompatibilityCheck.Severity.BLOCKING, abi.severity);
        assertEquals(CompatibilityCheck.Fix.IMPORT_GAME, abi.fix);
    }

    @Test
    public void theAbiCheckIsNotJudgedWithoutAVersion() {
        CompatibilityCheck.Facts facts = new CompatibilityCheck.Facts()
                .freeBytes(64L * 1024L * 1024L * 1024L)
                .abiCompatible(false)
                .versionSelected(false);
        CompatibilityCheck.Result abi = byId(CompatibilityCheck.evaluate(facts),
                CompatibilityCheck.Id.ABI);
        assertEquals("with no APK chosen there is nothing to judge", CompatibilityCheck.Severity.OK,
                abi.severity);
    }

    @Test
    public void thePlayStoreInstallWarnsWithAnImportFix() {
        CompatibilityCheck.Facts facts = new CompatibilityCheck.Facts()
                .freeBytes(64L * 1024L * 1024L * 1024L)
                .playStoreInstall(true);
        CompatibilityCheck.Result play = byId(CompatibilityCheck.evaluate(facts),
                CompatibilityCheck.Id.PLAY_STORE_INSTALL);
        assertEquals(CompatibilityCheck.Severity.WARNING, play.severity);
        assertEquals(CompatibilityCheck.Fix.IMPORT_GAME, play.fix);
        assertFalse("the Play Store build is a warning, not a blocker", play.isBlocking());
    }

    @Test
    public void aDeniedStoragePermissionBlocksWithAPermissionFix() {
        CompatibilityCheck.Facts facts = new CompatibilityCheck.Facts()
                .freeBytes(64L * 1024L * 1024L * 1024L)
                .storagePermissionGranted(false);
        CompatibilityCheck.Result perm = byId(CompatibilityCheck.evaluate(facts),
                CompatibilityCheck.Id.STORAGE_PERMISSION);
        assertEquals(CompatibilityCheck.Severity.BLOCKING, perm.severity);
        assertEquals(CompatibilityCheck.Fix.OPEN_PERMISSIONS, perm.fix);
    }

    @Test
    public void nullFactsRendersEveryRowWithoutBlocking() {
        // A missing reading is a warning, never a blocker and never a false pass: the screen must
        // still render, and an unknown free-space figure must not be reported as "all fine".
        List<CompatibilityCheck.Result> results = CompatibilityCheck.evaluate(null);
        assertEquals(4, results.size());
        assertFalse(CompatibilityCheck.hasBlocking(results));
        assertFalse(CompatibilityCheck.allOk(results));
    }

    @Test
    public void theSizeFormattingIsReadableAndUnknownIsNotZero() {
        assertEquals("unknown", CompatibilityCheck.formatBytes(CompatibilityCheck.UNKNOWN_BYTES));
        assertEquals("512 B", CompatibilityCheck.formatBytes(512L));
        assertEquals("1 KB", CompatibilityCheck.formatBytes(1024L));
        assertEquals("2.0 GB", CompatibilityCheck.formatBytes(2L * 1024L * 1024L * 1024L));
    }
}
