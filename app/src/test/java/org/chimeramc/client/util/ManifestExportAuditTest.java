package org.chimeramc.client.util;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Every exported component is a deliberate decision.
 *
 * <p>Reads the real manifest and fails when a component is exported without a recorded reason, so a
 * new {@code android:exported="true"} cannot slip in un-reviewed. The complementary direction is
 * also checked: the audit must not name a component the manifest does not export.
 */
public class ManifestExportAuditTest {

    /** One {@code <activity|service|receiver|provider ...>} element with its exported attribute. */
    private static final Pattern COMPONENT = Pattern.compile(
            "<(activity|service|receiver|provider|activity-alias)\\b([^>]*?)>", Pattern.DOTALL);

    @Test
    public void everyExportedComponentHasARecordedReason() throws IOException {
        Set<String> exported = exportedComponentNames();
        List<String> undocumented = new ArrayList<>();
        for (String name : exported) {
            if (ManifestExportAudit.reasonFor(name) == null) {
                undocumented.add(name);
            }
        }
        assertTrue("a component is exported without a recorded reason in ManifestExportAudit; add it "
                + "with its justification, which is where its caller validation is reviewed. "
                + "Undocumented: " + undocumented, undocumented.isEmpty());
    }

    @Test
    public void theAuditNamesOnlyComponentsTheManifestActuallyExports() throws IOException {
        Set<String> exported = exportedComponentNames();
        List<String> stale = new ArrayList<>();
        for (String name : ManifestExportAudit.exportedNames()) {
            if (!exported.contains(name)) {
                stale.add(name);
            }
        }
        assertTrue("ManifestExportAudit names a component the manifest does not export; the list has "
                + "drifted. Stale: " + stale, stale.isEmpty());
    }

    private static Set<String> exportedComponentNames() throws IOException {
        String xml = read(resolve("app/src/main/AndroidManifest.xml"));
        Set<String> out = new LinkedHashSet<>();
        Matcher m = COMPONENT.matcher(xml);
        while (m.find()) {
            String attrs = m.group(2);
            if (!attrs.contains("android:exported=\"true\"")) continue;
            String name = attribute(attrs, "android:name");
            if (name != null) out.add(name);
        }
        return out;
    }

    private static String attribute(String attrs, String name) {
        Matcher m = Pattern.compile(name + "=\"([^\"]+)\"").matcher(attrs);
        return m.find() ? m.group(1) : null;
    }

    private static Path resolve(String relativePath) throws IOException {
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            Path candidate = dir.resolve(relativePath);
            if (Files.exists(candidate)) return candidate;
            dir = dir.getParent();
        }
        throw new IllegalStateException("could not find " + relativePath);
    }

    private static String read(Path p) throws IOException {
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }
}
