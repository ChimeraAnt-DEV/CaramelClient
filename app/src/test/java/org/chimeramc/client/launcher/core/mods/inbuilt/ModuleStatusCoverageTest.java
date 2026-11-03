package org.chimeramc.client.launcher.core.mods.inbuilt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Every module the provider registers has a row in {@code docs/MODULE_STATUS.md}, and no row is
 * stale.
 *
 * <p>The status doc is the honest answer to "does this module work on this build?". It is only worth
 * anything if it covers exactly the modules that ship: a module added without a row is undocumented,
 * and a row for a module that no longer exists claims support for something that is gone. Both read
 * as an accurate list to someone who trusts it, which is the failure this guards against.
 *
 * <p>The ids are read from the real source — the provider's {@code create(...)} calls and the
 * {@code ModIds} constants they name — so a rename cannot leave the two out of step.
 */
public class ModuleStatusCoverageTest {

    private static final Pattern CREATE =
            Pattern.compile("mods\\.add\\(create\\([^,]+,\\s*[^,]+,\\s*[^,]+,\\s*ModIds\\.(\\w+)");
    private static final Pattern CONSTANT =
            Pattern.compile("public static final String (\\w+)\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern DOC_ROW =
            Pattern.compile("^\\|\\s*`([a-z0-9_]+)`\\s*\\|", Pattern.MULTILINE);

    @Test
    public void everyRegisteredModuleHasAStatusRowAndNoRowIsStale() throws IOException {
        Set<String> registered = registeredModuleIds();
        Set<String> documented = documentedModuleIds();

        Set<String> missing = new TreeSet<>(registered);
        missing.removeAll(documented);
        Set<String> stale = new TreeSet<>(documented);
        stale.removeAll(registered);

        assertTrue("these modules ship but have no row in docs/MODULE_STATUS.md: " + missing,
                missing.isEmpty());
        assertTrue("these rows describe modules that are no longer registered: " + stale,
                stale.isEmpty());
        assertEquals("the status doc and the provider must describe the same set",
                registered, documented);
    }

    private static Set<String> registeredModuleIds() throws IOException {
        Path provider = projectRoot().resolve(
                "app/src/main/java/org/chimeramc/client/launcher/core/mods/inbuilt/"
                        + "InbuiltModuleProvider.java");
        Path modIds = projectRoot().resolve(
                "app/src/main/java/org/chimeramc/client/launcher/core/mods/inbuilt/model/ModIds.java");
        String providerText = read(provider);
        String modIdsText = read(modIds);

        Map<String, String> constants = new HashMap<>();
        Matcher c = CONSTANT.matcher(modIdsText);
        while (c.find()) constants.put(c.group(1), c.group(2));

        Set<String> ids = new LinkedHashSet<>();
        Matcher m = CREATE.matcher(providerText);
        while (m.find()) {
            ids.add(constants.getOrDefault(m.group(1), m.group(1)));
        }
        return ids;
    }

    private static Set<String> documentedModuleIds() throws IOException {
        String doc = read(projectRoot().resolve("docs/MODULE_STATUS.md"));
        Set<String> ids = new LinkedHashSet<>();
        Matcher m = DOC_ROW.matcher(doc);
        while (m.find()) ids.add(m.group(1));
        return ids;
    }

    private static String read(Path p) throws IOException {
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }

    private static Path projectRoot() {
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            if (Files.exists(dir.resolve("app/src/main/res/layout"))) return dir;
            dir = dir.getParent();
        }
        throw new IllegalStateException("could not find the project root");
    }
}
