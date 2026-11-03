package org.chimeramc.client.core.mods;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The launcher stays usable when the mods folder is empty, a mod is corrupt, or a mod was built for
 * a newer game version.
 *
 * <p>These are the three states a user actually reaches by accident, and each one has to end in a
 * named refusal rather than a crash or a half-loaded mod: an empty folder is not an error at all, a
 * corrupt entry is refused before it reaches the linker, and a version mismatch is refused with a
 * reason the UI can show. The test drives the same pure decisions the loader makes, so it proves the
 * outcome without a device.
 */
public class ModLoadingRobustnessTest {

    private static Mod nativeMod(String id, List<String> versions, String pinnedSha, boolean verified) {
        return new Mod(id, "lib" + id + ".so", "lib" + id + ".so", id,
                versions, "author", "1.0.0", null, null, null,
                "/mods/" + id, "/mods/" + id + "/config", false, 0,
                pinnedSha == null ? "" : pinnedSha, verified, true, 0);
    }

    @Test
    public void anEmptyModsFolderIsNotAnError() {
        // Nothing to load is a valid state: no refusal, and nothing throws.
        assertTrue(ModNativeLoader.isCompatibleWithMinecraftVersion(
                Collections.<String>emptyList(), "1.26.60"));
        assertEquals(0, Mod.countEnabled(Collections.<Mod>emptyList()));
        assertEquals(0, Mod.countEnabled(null));
    }

    @Test
    public void aCorruptModIsRefusedBeforeItReachesTheLinker() {
        String pin = "cd".repeat(32);
        Mod tampered = nativeMod("corrupt", Collections.<String>emptyList(), pin, false);
        // The pinned hash exists but does not match the file: refused, not loaded.
        assertFalse(ModNativeLoader.mayLoad(tampered));
        // And the reason is classified so the Mods tab can badge it.
        assertEquals(ModLoadDiagnostics.KIND_HASH_MISMATCH,
                ModLoadDiagnostics.Classifier.kindOf(
                        new RuntimeException("pinned sha256 hash does not match")));
    }

    @Test
    public void aModBuiltForANewerGameVersionIsRefusedWithAReason() {
        Mod future = nativeMod("future", Arrays.asList("1.30.*"), null, false);
        assertFalse(ModNativeLoader.isCompatibleWithMinecraftVersion(future, "1.26.60"));
        // A wildcard for the running build still loads, so the rule is not "any version list fails".
        Mod current = nativeMod("current", Arrays.asList("1.26.*"), null, false);
        assertTrue(ModNativeLoader.isCompatibleWithMinecraftVersion(current, "1.26.60"));
    }

    @Test
    public void anUnpinnedModLoadsAndNoDecisionThrows() {
        // No version list and no pin: the most permissive case, and it must not throw.
        Mod plain = nativeMod("plain", null, null, false);
        assertTrue(ModNativeLoader.mayLoad(plain));
        assertTrue(ModNativeLoader.isCompatibleWithMinecraftVersion(plain, "1.26.60"));

        // A null version string is a refusal, not a crash.
        assertFalse(ModNativeLoader.isCompatibleWithMinecraftVersion(
                Arrays.asList("1.26.*"), null));
        assertNotNull(ModLoadDiagnostics.Classifier.describe(
                new UnsatisfiedLinkError("dlopen failed: library not found")));
    }

    @Test
    public void aMixedFolderStillLoadsTheGoodMods() {
        Mod good = nativeMod("good", Arrays.asList("1.26.*"), null, false);
        Mod wrongVersion = nativeMod("wrong", Arrays.asList("1.30.*"), null, false);
        Mod corrupt = nativeMod("corrupt", null, "ef".repeat(32), false);

        assertTrue(ModNativeLoader.mayLoad(good));
        assertTrue(ModNativeLoader.isCompatibleWithMinecraftVersion(good, "1.26.60"));

        assertTrue(ModNativeLoader.mayLoad(wrongVersion));           // hash is fine ...
        assertFalse(ModNativeLoader.isCompatibleWithMinecraftVersion(wrongVersion, "1.26.60"));

        assertFalse(ModNativeLoader.mayLoad(corrupt));               // ... this one is not

        // Exactly one of the three survives both gates, so one bad mod does not take the others down.
        int loadable = 0;
        for (Mod mod : Arrays.asList(good, wrongVersion, corrupt)) {
            if (ModNativeLoader.mayLoad(mod)
                    && ModNativeLoader.isCompatibleWithMinecraftVersion(mod, "1.26.60")) {
                loadable++;
            }
        }
        assertEquals(1, loadable);
    }
}
