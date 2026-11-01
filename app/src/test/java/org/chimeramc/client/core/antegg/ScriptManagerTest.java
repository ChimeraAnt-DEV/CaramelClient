package org.chimeramc.client.core.antegg;

import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Pins the script manager read model (item 226).
 *
 * <p>The manager is what the screen renders, so the properties worth pinning are the ones a user
 * would notice: an installed package appears with its declared metadata, a dependency that is
 * missing or out of range is reported rather than assumed satisfied, a package whose manifest
 * cannot be read is still listed so it can be removed, and the load order matches the loader's own
 * ordering rather than an alphabetical one that would disagree with it.
 *
 * <p>Real archives are used because the model reads the manifests out of the sandbox directories,
 * and a fake in-memory list would not prove the read path works.
 */
public class ScriptManagerTest {

    private static File sandbox() throws Exception {
        File dir = Files.createTempDirectory("antegg-sandbox").toFile();
        dir.deleteOnExit();
        return dir;
    }

    /** Installs a package into the sandbox by extracting a real archive, as the loader would. */
    private static void install(File sandbox, String name, String version, String type,
                                String entryPoint, String dependencies) throws Exception {
        File zip = File.createTempFile("antegg-manager", ".antegg");
        zip.deleteOnExit();
        String json = "{"
                + "\"name\":\"" + name + "\","
                + "\"version\":\"" + version + "\","
                + "\"author\":\"Someone\","
                + "\"type\":\"" + type + "\","
                + "\"entry_point\":\"" + entryPoint + "\","
                + "\"dependencies\":[" + dependencies + "]"
                + "}";
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
            out.putNextEntry(new ZipEntry("egg.json"));
            out.write(json.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
            out.putNextEntry(new ZipEntry(entryPoint));
            out.write("-- test entry\n".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
        AntEggPackage.Result result = AntEggPackage.extract(zip, sandbox);
        assertTrue("fixture must extract: " + result.error, result.isValid());
    }

    @Test
    public void anEmptyOrMissingSandboxYieldsNoRows() throws Exception {
        assertTrue(ScriptManager.list(null).isEmpty());
        assertTrue(ScriptManager.list(new File("/does/not/exist")).isEmpty());
        assertTrue(ScriptManager.list(sandbox()).isEmpty());
    }

    @Test
    public void anInstalledScriptAppearsWithItsManifestMetadata() throws Exception {
        File sandbox = sandbox();
        install(sandbox, "My Widget", "2.1.0", "script", "main.lua", "");

        List<ScriptManager.ScriptInfo> rows = ScriptManager.list(sandbox);
        assertEquals(1, rows.size());
        ScriptManager.ScriptInfo row = rows.get(0);
        assertTrue(row.readable);
        assertTrue(row.isScript());
        assertEquals("my-widget", row.id);
        assertEquals("My Widget", row.name);
        assertEquals("2.1.0", row.version);
        assertEquals("main.lua", row.entryPoint);
    }

    @Test
    public void aNativePackageIsListedButNotMarkedScript() throws Exception {
        File sandbox = sandbox();
        install(sandbox, "Native Core", "1.0.0", "native", "bin/libcore.so", "");

        List<ScriptManager.ScriptInfo> rows = ScriptManager.list(sandbox);
        assertEquals(1, rows.size());
        assertFalse(rows.get(0).isScript());
    }

    @Test
    public void aMissingDependencyIsReportedUnmet() throws Exception {
        File sandbox = sandbox();
        install(sandbox, "Plugin", "1.0.0", "script", "main.lua", "\"Core Lib>=1.2.0\"");

        ScriptManager.ScriptInfo row = ScriptManager.list(sandbox).get(0);
        assertEquals(1, row.dependencies.size());
        ScriptManager.DependencyStatus dep = row.dependencies.get(0);
        assertFalse(dep.met);
        assertEquals("core-lib", dep.id);
        assertEquals(">=1.2.0", dep.range);
        assertEquals(null, dep.installedVersion);
        assertFalse(row.allDependenciesMet());
    }

    @Test
    public void anInstalledDependencySatisfyingTheRangeIsMet() throws Exception {
        File sandbox = sandbox();
        install(sandbox, "Core Lib", "1.3.0", "script", "main.lua", "");
        install(sandbox, "Plugin", "1.0.0", "script", "main.lua", "\"Core Lib>=1.2.0\"");

        ScriptManager.ScriptInfo plugin = findById(ScriptManager.list(sandbox), "plugin");
        assertNotNull(plugin);
        assertTrue(plugin.allDependenciesMet());
        assertEquals("1.3.0", plugin.dependencies.get(0).installedVersion);
    }

    @Test
    public void anInstalledDependencyOutsideTheRangeIsUnmet() throws Exception {
        File sandbox = sandbox();
        install(sandbox, "Core Lib", "1.1.0", "script", "main.lua", "");
        install(sandbox, "Plugin", "1.0.0", "script", "main.lua", "\"Core Lib>=1.2.0\"");

        ScriptManager.ScriptInfo plugin = findById(ScriptManager.list(sandbox), "plugin");
        assertNotNull(plugin);
        assertFalse(plugin.allDependenciesMet());
        assertEquals("1.1.0", plugin.dependencies.get(0).installedVersion);
    }

    @Test
    public void aPlainIdDependencyWithNoRangeIsMetWhenPresent() throws Exception {
        File sandbox = sandbox();
        install(sandbox, "Core Lib", "0.0.1", "script", "main.lua", "");
        install(sandbox, "Plugin", "1.0.0", "script", "main.lua", "\"Core Lib\"");

        ScriptManager.ScriptInfo plugin = findById(ScriptManager.list(sandbox), "plugin");
        assertNotNull(plugin);
        assertTrue(plugin.allDependenciesMet());
    }

    @Test
    public void aPackageWithAnUnreadableManifestIsStillListed() throws Exception {
        File sandbox = sandbox();
        File broken = new File(sandbox, "broken-pack");
        assertTrue(broken.mkdirs());

        List<ScriptManager.ScriptInfo> rows = ScriptManager.list(sandbox);
        assertEquals(1, rows.size());
        assertFalse(rows.get(0).readable);
        assertEquals("broken-pack", rows.get(0).id);
    }

    @Test
    public void theLoadIndexPutsADependencyBeforeItsDependent() throws Exception {
        File sandbox = sandbox();
        install(sandbox, "Plugin", "1.0.0", "script", "main.lua", "\"Core Lib\"");
        install(sandbox, "Core Lib", "1.0.0", "script", "main.lua", "");

        List<ScriptManager.ScriptInfo> rows = ScriptManager.list(sandbox);
        ScriptManager.ScriptInfo lib = findById(rows, "core-lib");
        ScriptManager.ScriptInfo plugin = findById(rows, "plugin");
        assertNotNull(lib);
        assertNotNull(plugin);
        assertTrue("dependency must load first", lib.loadIndex < plugin.loadIndex);
    }

    @Test
    public void everyRowGetsADistinctLoadIndex() throws Exception {
        File sandbox = sandbox();
        install(sandbox, "Alpha", "1.0.0", "script", "main.lua", "");
        install(sandbox, "Beta", "1.0.0", "script", "main.lua", "");
        install(sandbox, "Gamma", "1.0.0", "script", "main.lua", "");

        List<ScriptManager.ScriptInfo> rows = ScriptManager.list(sandbox);
        assertEquals(3, rows.size());
        boolean[] seen = new boolean[3];
        for (ScriptManager.ScriptInfo row : rows) {
            assertFalse("load index " + row.loadIndex + " used twice",
                    seen[row.loadIndex]);
            seen[row.loadIndex] = true;
        }
    }

    @Test
    public void anUnreadableRowIsNeverRun() {
        ScriptManager.ScriptInfo broken = new ScriptManager.ScriptInfo(
                "broken", "broken", "", "", "", "", false,
                java.util.Collections.emptyList(), 0);
        assertFalse(ScriptManager.shouldRun(broken, null));
    }

    @Test
    public void aNativeRowIsNeverRunAsAScript() throws Exception {
        File sandbox = sandbox();
        install(sandbox, "Native Core", "1.0.0", "native", "bin/libcore.so", "");
        ScriptManager.ScriptInfo row = ScriptManager.list(sandbox).get(0);
        assertFalse(ScriptManager.shouldRun(row, null));
    }

    @Test
    public void aDisabledScriptIsNotRunAndAnEnabledOneIs() throws Exception {
        File sandbox = sandbox();
        install(sandbox, "Widget", "1.0.0", "script", "main.lua", "");
        ScriptManager.ScriptInfo row = ScriptManager.list(sandbox).get(0);
        assertTrue(ScriptManager.shouldRun(row, java.util.Collections.emptySet()));
        assertFalse(ScriptManager.shouldRun(row,
                java.util.Collections.singleton("widget")));
    }

    @Test
    public void runEnabledActuallyRunsTheInstalledScript() throws Exception {
        File sandbox = sandbox();
        install(sandbox, "Widget", "1.0.0", "script", "main.lua", "");
        // The fixture writes a valid Lua entry, so the run must succeed.
        List<ScriptManager.RunOutcome> outcomes =
                ScriptManager.runEnabled(sandbox, java.util.Collections.emptySet());
        assertEquals(1, outcomes.size());
        assertTrue(outcomes.get(0).error, outcomes.get(0).success);
    }

    @Test
    public void runEnabledSkipsDisabledScripts() throws Exception {
        File sandbox = sandbox();
        install(sandbox, "Widget", "1.0.0", "script", "main.lua", "");
        List<ScriptManager.RunOutcome> outcomes =
                ScriptManager.runEnabled(sandbox, java.util.Collections.singleton("widget"));
        assertTrue(outcomes.isEmpty());
    }

    private static ScriptManager.ScriptInfo findById(List<ScriptManager.ScriptInfo> rows,
                                                     String id) {
        for (ScriptManager.ScriptInfo row : rows) {
            if (id.equals(row.id)) return row;
        }
        return null;
    }
}
