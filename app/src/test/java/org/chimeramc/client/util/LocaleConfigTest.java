package org.chimeramc.client.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The system per-app language list stays in step with the shipped translations.
 *
 * <p>On API 33+ the platform reads {@code res/xml/locales_config.xml} and shows the app in the
 * system's App languages screen. A locale the app ships but the file omits is simply missing from
 * that screen, and a locale the file names but the app does not ship is offered and then does
 * nothing when picked. Both are silent; this test is what makes them loud.
 *
 * <p>Reads the real resource tree, like the other scale tests, so a new {@code values-<locale>}
 * folder is a build failure until it is added here and to the file.
 */
public class LocaleConfigTest {

    private static final Path RES = Paths.get("src", "main", "res");
    private static final Pattern LOCALE_ENTRY =
            Pattern.compile("<locale\\s+android:name=\"([^\"]+)\"");

    /** Qualifiers that are not languages, so a folder carrying one is not a locale folder. */
    private static final Set<String> NON_LOCALE_QUALIFIERS = new LinkedHashSet<>(java.util.List.of(
            "night", "sw840dp", "v29", "v31", "v35", "land", "port"));

    @Test
    public void everyShippedLocaleIsListedInTheSystemConfig() throws IOException {
        Set<String> shipped = shippedLocales();
        Set<String> configured = configuredLocales();

        Set<String> missingFromConfig = new TreeSet<>(shipped);
        missingFromConfig.removeAll(configured);
        assertTrue("a shipped locale must be in res/xml/locales_config.xml or it is missing from the "
                + "system App languages screen: " + missingFromConfig, missingFromConfig.isEmpty());
    }

    @Test
    public void theConfigNamesNoLocaleTheAppDoesNotShip() throws IOException {
        Set<String> shipped = shippedLocales();
        Set<String> configured = configuredLocales();

        Set<String> offeredButMissing = new TreeSet<>(configured);
        // "en" is the default resource folder (values/), so it has no values-en sibling.
        offeredButMissing.remove("en");
        offeredButMissing.removeAll(shipped);
        assertTrue("the config offers a locale with no resources, so picking it does nothing: "
                + offeredButMissing, offeredButMissing.isEmpty());
    }

    @Test
    public void englishIsAlwaysOffered() throws IOException {
        assertTrue("English is the default resources and must be selectable",
                configuredLocales().contains("en"));
    }

    /** The locales that have a {@code values-<locale>} resource folder, in BCP-47 form. */
    private static Set<String> shippedLocales() throws IOException {
        Set<String> out = new LinkedHashSet<>();
        try (DirectoryStream<Path> dirs = Files.newDirectoryStream(RES)) {
            for (Path dir : dirs) {
                if (!Files.isDirectory(dir)) continue;
                String name = dir.getFileName().toString();
                if (!name.startsWith("values-")) continue;
                String qualifier = name.substring("values-".length());
                if (NON_LOCALE_QUALIFIERS.contains(qualifier)) continue;
                out.add(toBcp47(qualifier));
            }
        }
        return out;
    }

    private static Set<String> configuredLocales() throws IOException {
        String xml = new String(Files.readAllBytes(RES.resolve("xml").resolve("locales_config.xml")),
                StandardCharsets.UTF_8);
        Set<String> out = new LinkedHashSet<>();
        Matcher m = LOCALE_ENTRY.matcher(xml);
        while (m.find()) out.add(m.group(1));
        return out;
    }

    /**
     * The Android resource qualifier uses {@code -r} before a region ({@code zh-rCN}); BCP-47 uses a
     * plain hyphen ({@code zh-CN}). The config file uses the BCP-47 form the platform expects.
     */
    private static String toBcp47(String qualifier) {
        return qualifier.replace("-r", "-");
    }
}
