package org.chimeramc.client.singularity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import java.io.File;

/**
 * Pins the fail-closed contract of the {@code .singularity} engine's Java shell, mirroring
 * {@link org.chimeramc.client.universe.UniverseLoaderTest}: with the Rust cdylib absent every entry
 * point returns a defined "unavailable" answer instead of throwing.
 */
public class SingularityEngineTest {

    @Test
    public void reportsUnavailableWhenTheLibraryIsAbsent() {
        assertFalse(SingularityEngine.isAvailable());
    }

    @Test
    public void everyFallibleEntryPointReturnsAMessageNotAThrow() {
        assertNotNull(SingularityEngine.init(0L));
        assertNotNull(SingularityEngine.validate(new File("/tmp/x.singularity")));
        assertNotNull(SingularityEngine.extract(new File("/tmp/x.singularity"), new File("/tmp/s"),
                new File("/tmp")));
        assertNotNull(SingularityEngine.parseManifest("{}"));
    }

    @Test
    public void nullArgumentsAreRefusedWithoutThrowing() {
        assertNotNull(SingularityEngine.validate(null));
        assertNotNull(SingularityEngine.extract(null, null, null));
        assertNotNull(SingularityEngine.parseManifest(null));
    }

    @Test
    public void predicateAndVoidEntryPointsDegradeSafely() {
        assertFalse(SingularityEngine.looksLikeSingularity("sample.singularity"));
        SingularityEngine.setCancelled(true);
    }

    @Test
    public void logTagMatchesTheRustSide() {
        assertEquals("SweetLoader.Singularity", SingularityEngine.LOG_TAG);
    }
}
