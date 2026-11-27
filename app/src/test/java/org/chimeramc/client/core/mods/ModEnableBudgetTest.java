package org.chimeramc.client.core.mods;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The native mod enable never runs on the main thread.
 *
 * <p>{@code enableLoadedMods()} used to run the native enable on a worker and then have the game
 * activity's <em>main</em> thread wait up to {@link ModManager#ENABLE_BUDGET_MS} for it. That was
 * still a freeze: the loading screen had already called {@code startActivity()+finish()}, so the
 * last painted status stayed on screen while the game activity blocked. The call is now
 * fire-and-forget, and a worker-side timer records a timeout so a stuck mod stays visible.
 */
public class ModEnableBudgetTest {

    @Test
    public void theEnableRunsOffTheCallingThreadAndDoesNotWait() throws IOException {
        String source = read("app/src/main/java/org/chimeramc/client/launcher/core/mods/"
                + "ModManager.java");
        int start = source.indexOf("public static void enableLoadedMods()");
        assertTrue("enableLoadedMods must exist", start >= 0);
        int end = source.indexOf("public static void enableLoadedEngineMods(", start);
        assertTrue("the enable helpers must follow the enable", end > start);
        String body = source.substring(start, end);
        assertTrue("the native enable must run on a background thread",
                body.contains("AppExecutors.thread("));
        assertFalse("the caller must not wait on the main thread",
                body.contains("done.await("));
        assertTrue("a timeout must be recorded by a worker-side timer, not a main-thread wait",
                body.contains("AppExecutors.timer().schedule("));
        assertTrue("a timeout must be recorded rather than thrown",
                body.contains("recordEnableTimeout()"));
        assertTrue("the budget must be a positive constant", ModManager.ENABLE_BUDGET_MS > 0L);
    }

    @Test
    public void theGameActivityDispatchesTheEnableAfterSuperOnCreate() throws IOException {
        String activity = read("app/src/main/java/org/chimeramc/client/launcher/core/minecraft/"
                + "MinecraftActivity.kt");
        int superOnCreate = activity.indexOf("super.onCreate(savedInstanceState)");
        int modEnable = activity.indexOf("ModManager.enableLoadedMods()");
        int engineEnable = activity.indexOf("ModManager.enableLoadedEngineMods(");
        assertTrue("the activity must call super.onCreate", superOnCreate >= 0);
        assertTrue("the activity must enable the loaded mods", modEnable >= 0);
        assertTrue("the activity must enable the engine mods", engineEnable >= 0);
        assertTrue("the native enable must not run before the game activity is created",
                modEnable > superOnCreate);
        assertTrue("the engine enable must not run before the game activity is created",
                engineEnable > superOnCreate);
    }

    @Test
    public void theTimeoutIsObservableForDiagnostics() throws IOException {
        String source = read("app/src/main/java/org/chimeramc/client/launcher/core/mods/"
                + "ModManager.java");
        assertTrue("the timeout state must be readable",
                source.contains("public static boolean lastEnableTimedOut()"));
        assertTrue("the timeout state must be volatile",
                source.contains("private static volatile boolean enableTimedOut;"));
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(root().resolve(relativePath)), StandardCharsets.UTF_8);
    }

    private static Path root() {
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            if (Files.exists(dir.resolve("app/src/main/res/layout"))) return dir;
            dir = dir.getParent();
        }
        throw new IllegalStateException("could not find the project root");
    }
}
