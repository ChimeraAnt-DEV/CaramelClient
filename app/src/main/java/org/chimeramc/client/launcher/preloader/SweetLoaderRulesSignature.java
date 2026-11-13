package org.chimeramc.client.preloader;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Verifies a detached signature over a sweetloader rule file.
 *
 * <p>The rules are delivered as a data file so a new Minecraft build needs no app release. That
 * makes the file remote input, and remote input that decides which code addresses get patched is
 * the most dangerous kind there is: a tampered rule file would point the hooks at arbitrary
 * addresses. The file is therefore signed, and the client holds only the public key, so a
 * compromised host or a man in the middle cannot produce a rule set this build will accept.
 *
 * <p>Deliberately not a hand-rolled scheme. It is RSA-SHA256 through {@link java.security.Signature},
 * the platform's own verifier, so there is no bespoke padding or comparison code to get wrong, and
 * the format is one every signing tool already speaks.
 *
 * <p>Fail-closed: a missing key, a missing signature, malformed base64, or any exception at all
 * reads as "not verified". The caller then keeps the bundled rules rather than installing an
 * unverified set.
 */
public final class SweetLoaderRulesSignature {

    /** The JCA algorithm and the digest it wraps, named once so signer and verifier cannot drift. */
    public static final String ALGORITHM = "SHA256withRSA";

    private SweetLoaderRulesSignature() {
    }

    /**
     * Verifies {@code payload} against {@code signatureBase64} with the key in {@code publicKeyPem}.
     *
     * <p>Returns false for every failure mode, including a null or blank argument, so a caller can
     * treat the answer as a plain yes/no without a second error channel.
     */
    public static boolean verify(byte[] payload, String signatureBase64, String publicKeyPem) {
        if (payload == null || payload.length == 0) return false;
        if (signatureBase64 == null || signatureBase64.trim().isEmpty()) return false;
        if (publicKeyPem == null || publicKeyPem.trim().isEmpty()) return false;

        byte[] signatureBytes;
        try {
            signatureBytes = Base64.getMimeDecoder().decode(signatureBase64.trim());
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (signatureBytes.length == 0) return false;

        PublicKey key = parsePublicKey(publicKeyPem);
        if (key == null) return false;

        try {
            Signature verifier = Signature.getInstance(ALGORITHM);
            verifier.initVerify(key);
            verifier.update(payload);
            return verifier.verify(signatureBytes);
        } catch (Exception | LinkageError e) {
            return false;
        }
    }

    /** Verifies a UTF-8 payload, which is what a rule file is. */
    public static boolean verify(String payload, String signatureBase64, String publicKeyPem) {
        if (payload == null) return false;
        return verify(payload.getBytes(StandardCharsets.UTF_8), signatureBase64, publicKeyPem);
    }

    /**
     * Parses an X.509 SubjectPublicKeyInfo PEM into a key, or null when it is not one.
     *
     * <p>The header and footer are stripped by hand rather than through a PEM library, because the
     * only PEM this code ever sees is the one this project ships and a full parser would be more
     * code than the format it reads.
     */
    public static PublicKey parsePublicKey(String publicKeyPem) {
        if (publicKeyPem == null) return null;
        String body = publicKeyPem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        if (body.isEmpty()) return null;
        try {
            byte[] der = Base64.getDecoder().decode(body);
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception | LinkageError e) {
            return null;
        }
    }

    /**
     * True when the PEM actually holds a usable RSA public key, so a mis-shipped asset is caught by
     * a test rather than by a silent "every remote rule set is refused" in the field.
     */
    public static boolean isUsableKey(String publicKeyPem) {
        return parsePublicKey(publicKeyPem) != null;
    }
}
