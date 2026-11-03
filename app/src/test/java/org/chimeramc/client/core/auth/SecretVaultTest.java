package org.chimeramc.client.core.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The rules of the Keystore vault that do not need a device.
 *
 * <p>The Keystore itself only exists on Android, so the round-trip is a device test. What is
 * pinned here is the classification and the failure contract: a value is either recognisably ours
 * or it is legacy plaintext, and a value that cannot be decrypted reads as absent rather than as
 * garbage. Those two rules are what stop an account from being silently corrupted.
 */
public class SecretVaultTest {

    @Test
    public void aPlainValueIsNotEncrypted() {
        assertFalse(SecretVault.isEncrypted("not-a-vault-value"));
        assertFalse(SecretVault.isEncrypted(null));
        assertFalse(SecretVault.isEncrypted(""));
    }

    @Test
    public void thePrefixMarksCiphertext() {
        assertTrue(SecretVault.isEncrypted(SecretVault.PREFIX + "AAAA"));
    }

    @Test
    public void decryptPassesLegacyPlaintextThrough() {
        // An account written before this change must keep working, not read as signed out.
        assertEquals("{\"token\":\"legacy\"}", SecretVault.decrypt("{\"token\":\"legacy\"}"));
    }

    @Test
    public void decryptReturnsNullForGarbageCiphertext() {
        // A value we cannot decrypt must read as "no secret", so the caller signs out cleanly.
        assertNull(SecretVault.decrypt(SecretVault.PREFIX + "!!!not-base64!!!"));
        assertNull(SecretVault.decrypt(SecretVault.PREFIX));
    }

    @Test
    public void emptyAndNullRoundTripUnchanged() {
        assertNull(SecretVault.decrypt(null));
        assertEquals("", SecretVault.decrypt(""));
        assertNull(SecretVault.encrypt(null));
        assertEquals("", SecretVault.encrypt(""));
    }

    @Test
    public void encryptIsIdempotentOnCiphertext() {
        String already = SecretVault.PREFIX + "AAAA";
        assertEquals(already, SecretVault.encrypt(already));
    }
}
