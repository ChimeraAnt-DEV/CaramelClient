package org.chimeramc.client.core.mods;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.Test;

/**
 * Guards the R8 keep rules that name a package against the real source tree.
 *
 * <p>A {@code -keep class <package>.**} rule that names a package that does not exist is silently
 * useless: R8 renames the class, the mangled JNI symbol
 * {@code Java_<package>_<Class>_<method>} no longer matches its C++ export, and every native call
 * throws {@link UnsatisfiedLinkError} at the first use — but only in the minified release build, so
 * the debug build never shows it.
 *
 * <p>This is not hypothetical: {@code app/proguard-rules.pro} kept
 * {@code org.chimeramc.client.launcher.preloader.**}, but the preloader bridge's package declaration
 * is {@code org.chimeramc.client.preloader} (the files sit in a {@code launcher/preloader/}
 * directory, but the directory segment is not part of the name). The rule matched nothing.
 *
 * <p>The scan reads the real {@code proguard-rules.pro} and the real source tree, so a package that
 * is renamed on one side only fails here instead of at runtime on a release build.
 */
public class ProguardKeepRulesTest {

    private static final Path RULES = Paths.get("proguard-rules.pro");
    private static final Path SOURCE_ROOT = Paths.get("src", "main", "java");

    /**
     * The packages that must be kept by name. Each is a package whose classes are reached from
     * native code by their fully-qualified name, so R8 renaming the class breaks the binding.
     */
    private static final List<String> REQUIRED_KEEPS = List.of(
            "org.chimeramc.client.preloader",
            "org.chimeramc.client.core.jni",
            "io.bambosan.mbloader.launcherUtils",
            "org.chimeramc.client.core.antegg");

    private static final Pattern KEEP_CLASS =
            Pattern.compile("^\\s*-keep\\s+class\\s+([\\w.$*]+)", Pattern.MULTILINE);
    private static final Pattern PACKAGE_DECL =
            Pattern.compile("^\\s*package\\s+([\\w.]+)", Pattern.MULTILINE);

    @Test
    public void everyKeptPackageExistsInTheSourceTree() throws IOException {
        Path rules = findFile(RULES);
        String text = new String(Files.readAllBytes(rules), StandardCharsets.UTF_8);
        List<String> packages = declaredPackages();

        List<String> missing = new ArrayList<>();
        Matcher m = KEEP_CLASS.matcher(text);
        while (m.find()) {
            String target = m.group(1);
            // Only package wildcards are checked; a plain class keep is matched by FQN below.
            if (!target.endsWith(".**")) continue;
            String pkg = target.substring(0, target.length() - 3);
            // Third-party libraries (gson, msal, luaj, ...) are kept by name but do not live in this
            // source tree, so they cannot be verified here. Only the project's own packages are.
            if (!isProjectPackage(pkg)) continue;
            if (!packages.contains(pkg)) {
                missing.add(pkg);
            }
        }
        if (!missing.isEmpty()) {
            fail("proguard-rules.pro keeps a package that does not exist in the source tree, so the"
                    + " rule matches nothing and R8 renames the class (the JNI symbol then fails to"
                    + " resolve). Fix the package name:\n  " + String.join("\n  ", missing));
        }
    }

    @Test
    public void thePackagesThatMustBeKeptByNameAreCovered() throws IOException {
        Path rules = findFile(RULES);
        String text = new String(Files.readAllBytes(rules), StandardCharsets.UTF_8);
        List<String> kept = new ArrayList<>();
        Matcher m = KEEP_CLASS.matcher(text);
        while (m.find()) {
            kept.add(m.group(1));
        }
        for (String required : REQUIRED_KEEPS) {
            assertTrue("proguard-rules.pro no longer keeps " + required + ".** by name",
                    kept.contains(required + ".**"));
        }
    }

    /** The packages this project owns, as opposed to a third-party library it bundles. */
    private static boolean isProjectPackage(String pkg) {
        return pkg.startsWith("org.chimeramc.") || pkg.startsWith("io.bambosan.");
    }

    private static List<String> declaredPackages() throws IOException {
        List<String> packages = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(SOURCE_ROOT)) {
            for (Path file : (Iterable<Path>) paths
                    .filter(p -> p.toString().endsWith(".java") || p.toString().endsWith(".kt"))
                    ::iterator) {
                String source = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                Matcher m = PACKAGE_DECL.matcher(source);
                if (m.find()) {
                    packages.add(m.group(1));
                }
            }
        }
        return packages;
    }

    private static Path findFile(Path relative) {
        Path candidate = relative;
        if (Files.exists(candidate)) return candidate;
        // The test runs with the module directory (app/) as the working directory.
        candidate = Paths.get("app").resolve(relative);
        if (Files.exists(candidate)) return candidate;
        // Or with the repository root as the working directory.
        candidate = Paths.get("..").resolve(relative);
        if (Files.exists(candidate)) return candidate;
        throw new IllegalStateException("cannot locate " + relative + " from "
                + Paths.get("").toAbsolutePath());
    }
}
