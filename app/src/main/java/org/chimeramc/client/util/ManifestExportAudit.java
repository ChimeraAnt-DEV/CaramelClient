package org.chimeramc.client.util;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The audit of the launcher's exported manifest components (item 513).
 *
 * <p>An {@code android:exported="true"} component can be started by any other app on the device, so
 * every one of them is a deliberate decision with a reason, not a default. This class records the
 * decision next to the reason so the manifest can be checked against it: a new exported component
 * fails {@code ManifestExportAuditTest} until it is added here with its justification, which is the
 * point at which the caller validation is reviewed.
 *
 * <p>Pure and Android-free: it holds names and reasons, and the test reads the real manifest.
 */
public final class ManifestExportAudit {

    /** One exported component and why exporting it is intended. */
    public static final class Exported {
        public final String name;
        public final String reason;

        Exported(String name, String reason) {
            this.name = name;
            this.reason = reason;
        }
    }

    private static final Set<Exported> EXPORTED = build();

    private ManifestExportAudit() {
    }

    private static Set<Exported> build() {
        Set<Exported> out = new LinkedHashSet<>();
        out.add(new Exported("org.chimeramc.client.ui.activities.SplashActivity",
                "Launcher entry point. Starts with MAIN/LAUNCHER and reads no extras before it "
                        + "routes; the wizard and the main screen are opened by the splash itself."));
        out.add(new Exported("org.chimeramc.client.ui.activities.MainActivity",
                "Share target. Receives ACTION_SEND text/plain; the payload is filtered by "
                        + "AntEggLink and every extra is validated in IntentHandler before use, so a "
                        + "share of unrelated text is ignored rather than acted on."));
        out.add(new Exported(".ui.activities.IntentHandler",
                "Opens a game package or an .AntEgg from another app (ACTION_VIEW). Every extra is "
                        + "validated and size-limited by IntentHandler before it is used."));
        out.add(new Exported("com.microsoft.xal.browser.IntentHandler",
                "Xbox sign-in redirect (ms-xal-...://auth). Third-party SDK component: the scheme is "
                        + "the app's own XAL redirect, so only the sign-in flow can reach it."));
        return Collections.unmodifiableSet(out);
    }

    /** Every component that is intended to be exported, with its reason. */
    public static Set<Exported> exported() {
        return EXPORTED;
    }

    /** The names of the intended exported components. */
    public static Set<String> exportedNames() {
        Set<String> names = new LinkedHashSet<>();
        for (Exported e : EXPORTED) names.add(e.name);
        return names;
    }

    /** The reason a component is exported, or null when it is not on the intended list. */
    public static String reasonFor(String name) {
        for (Exported e : EXPORTED) {
            if (e.name.equals(name)) return e.reason;
        }
        return null;
    }
}
