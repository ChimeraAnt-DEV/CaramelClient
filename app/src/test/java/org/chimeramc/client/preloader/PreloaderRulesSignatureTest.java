package org.chimeramc.client.preloader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;

/**
 * Pins the detached-signature check on the preloader rule file.
 *
 * <p>The rule file decides which code addresses the preloader patches, so an unverified rule set is
 * remote code execution by proxy. The interesting cases are all refusals: a tampered payload, a
 * signature from another key, a truncated or malformed signature, and a missing key. Each must read
 * as "not verified" rather than throwing, because the caller treats a false as "keep the bundled
 * rules" and an exception there would abort the refresh.
 *
 * <p>Key generation is real RSA rather than a fixture, so a scheme change that breaks the verifier
 * (a different digest, a wrong encoding) fails here instead of in the field.
 */
public class PreloaderRulesSignatureTest {

    private static final String PAYLOAD = "{\"revision\":7,\"rules\":[]}";

    private static KeyPair newKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static String sign(KeyPair pair, String payload) throws Exception {
        Signature signer = Signature.getInstance(PreloaderRulesSignature.ALGORITHM);
        signer.initSign(pair.getPrivate());
        signer.update(payload.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signer.sign());
    }

    private static String pem(java.security.PublicKey key) {
        String body = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(key.getEncoded());
        return "-----BEGIN PUBLIC KEY-----\n" + body + "\n-----END PUBLIC KEY-----\n";
    }

    @Test
    public void aSignatureFromTheMatchingKeyVerifies() throws Exception {
        KeyPair pair = newKeyPair();
        assertTrue(PreloaderRulesSignature.verify(PAYLOAD, sign(pair, PAYLOAD), pem(pair.getPublic())));
    }

    @Test
    public void aTamperedPayloadIsRefused() throws Exception {
        KeyPair pair = newKeyPair();
        String signature = sign(pair, PAYLOAD);
        String tampered = PAYLOAD.replace("7", "8");
        assertFalse("a changed byte must invalidate the signature",
                PreloaderRulesSignature.verify(tampered, signature, pem(pair.getPublic())));
    }

    @Test
    public void aSignatureFromAnotherKeyIsRefused() throws Exception {
        KeyPair signingKey = newKeyPair();
        KeyPair otherKey = newKeyPair();
        assertFalse("only the shipped key's signature may be accepted",
                PreloaderRulesSignature.verify(PAYLOAD, sign(signingKey, PAYLOAD),
                        pem(otherKey.getPublic())));
    }

    @Test
    public void malformedInputsAreRefusedWithoutThrowing() throws Exception {
        KeyPair pair = newKeyPair();
        String key = pem(pair.getPublic());
        String good = sign(pair, PAYLOAD);

        assertFalse(PreloaderRulesSignature.verify((String) null, good, key));
        assertFalse(PreloaderRulesSignature.verify("", good, key));
        assertFalse(PreloaderRulesSignature.verify(PAYLOAD, null, key));
        assertFalse(PreloaderRulesSignature.verify(PAYLOAD, "", key));
        assertFalse(PreloaderRulesSignature.verify(PAYLOAD, "!!!not base64!!!", key));
        assertFalse(PreloaderRulesSignature.verify(PAYLOAD, good, null));
        assertFalse(PreloaderRulesSignature.verify(PAYLOAD, good, ""));
        assertFalse(PreloaderRulesSignature.verify(PAYLOAD, good, "-----BEGIN PUBLIC KEY-----\nnope\n-----END PUBLIC KEY-----"));
        // A truncated signature is still valid base64 but must not verify.
        assertFalse(PreloaderRulesSignature.verify(PAYLOAD,
                good.substring(0, good.length() / 2), key));
    }

    @Test
    public void theKeyParserAcceptsOnlyARealPublicKey() throws Exception {
        KeyPair pair = newKeyPair();
        assertNotNull(PreloaderRulesSignature.parsePublicKey(pem(pair.getPublic())));
        assertTrue(PreloaderRulesSignature.isUsableKey(pem(pair.getPublic())));
        assertNull(PreloaderRulesSignature.parsePublicKey("not a key"));
        assertNull(PreloaderRulesSignature.parsePublicKey(""));
        assertFalse(PreloaderRulesSignature.isUsableKey(null));
    }

    @Test
    public void theShippedKeyAndSignatureVerifyTheShippedRules() throws Exception {
        // The asset pair the app ships must actually work: a mistyped key or a signature made
        // before the last edit to the rules would silently disable every remote rule set.
        Path rules = repoFile("resources/preloader/preloader_signature_rules_source.json");
        Path signature = repoFile("resources/preloader/preloader_signature_rules_source.json.sig");
        Path key = repoFile("resources/preloader/preloader_rules_public_key.pem");
        org.junit.Assume.assumeTrue("rule assets are not checked out",
                rules != null && signature != null && key != null);

        String payload = new String(Files.readAllBytes(rules), StandardCharsets.UTF_8);
        String signatureText = new String(Files.readAllBytes(signature), StandardCharsets.UTF_8);
        String publicKey = new String(Files.readAllBytes(key), StandardCharsets.UTF_8);
        assertTrue("the bundled rules must verify against the bundled public key",
                PreloaderRulesSignature.verify(payload, signatureText, publicKey));

        // And the negative half: a one-character change to the rules must fail.
        String tampered = payload.replace("\"revision\"", "\"revision2\"");
        assertEquals("the tamper fixture must actually change the payload",
                payload.length() + 1, tampered.length());
        assertFalse(PreloaderRulesSignature.verify(tampered, signatureText, publicKey));
    }

    private static Path repoFile(String relative) {
        // Gradle runs unit tests with the module dir as the working directory.
        for (String prefix : new String[]{"../", ""}) {
            Path candidate = Paths.get(prefix + relative).toAbsolutePath().normalize();
            if (Files.exists(candidate)) return candidate;
        }
        return null;
    }
}
