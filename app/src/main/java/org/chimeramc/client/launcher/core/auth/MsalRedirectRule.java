package org.chimeramc.client.core.auth;

/**
 * The one place the MSAL redirect URI is derived.
 *
 * <p>MSAL's Android redirect is {@code msauth://<package>/<signature-hash>}. The runtime value the
 * library is given and the manifest intent-filter that receives Microsoft's reply must be built from
 * the same package and the same signature hash, or the interactive flow completes at Microsoft and
 * the reply is never delivered to the app — a sign-in that appears to hang.
 *
 * <p>Kept as a pure rule (no Android types, no native load) so both halves are unit-tested off a
 * device; the manifest placeholder is {@code ${msalSignatureHash}}, filled by Gradle with the value
 * this class produces at runtime.
 */
public final class MsalRedirectRule {

    private MsalRedirectRule() {
    }

    /** The scheme every MSAL Android redirect uses. */
    public static String manifestScheme() {
        return "msauth";
    }

    /** The manifest filter's host: the application package. */
    public static String manifestHost(String packageName) {
        return packageName;
    }

    /** The manifest filter's path: {@code /} followed by the signature hash. */
    public static String manifestPath(String signatureHash) {
        return "/" + (signatureHash == null ? "" : signatureHash);
    }

    /** The redirect URI, built so a null hash can never leak a {@code null} literal into it. */
    public static String redirectUri(String packageName, String signatureHash) {
        return manifestScheme() + "://" + packageName + manifestPath(signatureHash);
    }
}
