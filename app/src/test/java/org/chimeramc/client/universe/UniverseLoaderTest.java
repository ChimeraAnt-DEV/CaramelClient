package org.chimeramc.client.universe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import java.io.File;

/**
 * Pins the fail-closed contract of the {@code .universe} loader's Java shell.
 *
 * <p>On the build host the Rust cdylib is not on the JVM's library path, so every entry point must
 * degrade to a defined answer — an empty-string "unavailable" message, {@code false}, or an empty
 * version — rather than throwing {@link UnsatisfiedLinkError} into the caller. That is exactly the
 * behaviour a device gets when the loader ships without its library, so the rule the test pins is
 * the rule the launcher relies on.
 */
public class UniverseLoaderTest {

    @Test
    public void reportsUnavailableWhenTheLibraryIsAbsent() {
        // No libcaramel_universe.so is loadable in a host unit test, so this is the absent path.
        assertFalse(UniverseLoader.isAvailable());
    }

    @Test
    public void everyFallibleEntryPointReturnsAMessageNotAThrow() {
        assertNotNull(UniverseLoader.init(0L));
        assertNotNull(UniverseLoader.validate(new File("/tmp/x.universe")));
        assertNotNull(UniverseLoader.extract(new File("/tmp/x.universe"), new File("/tmp/s"),
                new File("/tmp")));
        assertNotNull(UniverseLoader.readManifest(new File("/tmp/x.universe")));
        assertNotNull(UniverseLoader.checkVersion("1.0.0"));
        assertNotNull(UniverseLoader.clientVersion());
    }

    @Test
    public void nullArgumentsAreRefusedWithoutThrowing() {
        assertNotNull(UniverseLoader.validate(null));
        assertNotNull(UniverseLoader.extract(null, null, null));
    }

    @Test
    public void predicateAndVoidEntryPointsDegradeSafely() {
        assertFalse(UniverseLoader.looksLikeUniverse("sample.universe"));
        // Must not throw when the library is absent.
        UniverseLoader.setCancelled(true);
    }

    @Test
    public void clientVersionIsEmptyWhenUnavailable() {
        assertEquals("", UniverseLoader.clientVersion());
    }
}
