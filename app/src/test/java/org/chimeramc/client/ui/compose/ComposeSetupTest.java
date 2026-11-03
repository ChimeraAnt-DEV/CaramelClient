package org.chimeramc.client.ui.compose;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The Compose toolchain stays wired.
 *
 * <p>Compose was absent from this project entirely, and a reverted line in the catalog or the Gradle
 * build would break every Compose screen with a confusing "no compiler plugin" error. This pins the
 * four pieces that must all be present: the compiler plugin applied, {@code buildFeatures.compose},
 * the BOM, and the Material3 dependency. A build machine cannot instantiate a composable, so the
 * check is on the real build files rather than on a rendered tree.
 */
public class ComposeSetupTest {

    @Test
    public void theCompilerPluginIsDeclaredAndApplied() throws IOException {
        assertTrue("the version catalog must declare the compose compiler plugin",
                read("gradle/libs.versions.toml").contains("org.jetbrains.kotlin.plugin.compose"));
        assertTrue("the root build must declare it apply-false",
                read("build.gradle").contains("libs.plugins.kotlin.compose"));
        assertTrue("the app must apply the compose compiler plugin",
                read("app/build.gradle").contains("alias(libs.plugins.kotlin.compose)"));
    }

    @Test
    public void theBuildEnablesComposeAndPinsTheBom() throws IOException {
        String app = read("app/build.gradle");
        assertTrue("buildFeatures.compose must be on", app.contains("compose true"));
        assertTrue("the Compose BOM must pin the compose versions",
                app.contains("platform(libs.compose.bom)"));
        assertTrue("Material 3 must be on the classpath", app.contains("libs.compose.material3"));
    }

    @Test
    public void theThemeAndDimensLiveInTheComposePackage() {
        assertTrue(Files.exists(root().resolve(
                "app/src/main/java/org/chimeramc/client/ui/compose/theme/GlowTheme.kt")));
        assertTrue(Files.exists(root().resolve(
                "app/src/main/java/org/chimeramc/client/ui/compose/theme/GlowPalette.java")));
        assertTrue(Files.exists(root().resolve(
                "app/src/main/java/org/chimeramc/client/ui/compose/theme/GlowDimens.java")));
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
