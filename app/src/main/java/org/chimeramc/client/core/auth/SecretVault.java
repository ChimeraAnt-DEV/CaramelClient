package org.chimeramc.client.core.auth;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import androidx.annotation.Nullable;

import org.chimeramc.client.util.AppLog;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Encrypts a secret at rest with a key that never leaves the Android Keystore (item 515).
 *
 * <p>The account store writes the serialized Microsoft auth manager — an OAuth refresh token plus
 * its session material — into a plain JSON file. On a rooted device or a debug build that file is
 * readable, and a copied token is a working session. Wrapping that one field in an AES/GCM
 * ciphertext whose key is held by the Keystore means the file on disk is useless without the
 * device's own hardware-backed key, which cannot be exported.
 *
 * <p>Deliberately narrow: this is not a general-purpose crypto helper. It encrypts and decrypts one
 * string, it returns {@code null} on any failure rather than throwing, and every caller treats
 * {@code null} as "no secret available" so a Keystore problem degrades to a signed-out state rather
 * than a crash. A value that is not in the {@value #PREFIX} form is treated as legacy plaintext, so
 * an account written by an older build still reads and is upgraded on the next write.
 *
 * <p>Keystore AES/GCM is available from API 23, below the app's {@code minSdk} of 28, so there is no
 * version gate here.
 */
public final class SecretVault {

    private static final String TAG = "SecretVault";

    /** Marks a value that is already ciphertext, so a legacy plaintext value is recognisable. */
    public static final String PREFIX = "ks1:";

    private static final String KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "glowberry.secret.vault.v1";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    /** 128-bit tag is the GCM default and is what the platform will accept. */
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_IV_BYTES = 12;

    private SecretVault() {
    }

    /**
     * Encrypts a value, or returns it unchanged when it is already ciphertext.
     *
     * <p>Returns the raw value when the Keystore is unavailable: refusing to persist the account at
     * all would lock a user out over a device quirk that is not their fault, and the field is still
     * no less protected than it was before.
     */
    @Nullable
    public static String encrypt(@Nullable String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) return plaintext;
        if (isEncrypted(plaintext)) return plaintext;
        try {
            SecretKey key = getOrCreateKey();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] iv = cipher.getIV();
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] packed = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, packed, 0, iv.length);
            System.arraycopy(ciphertext, 0, packed, iv.length, ciphertext.length);
            return PREFIX + Base64.encodeToString(packed, Base64.NO_WRAP);
        } catch (Throwable t) {
            AppLog.warn(TAG, "encrypt failed; storing the value as-is", t);
            return plaintext;
        }
    }

    /**
     * Decrypts a value, or returns it unchanged when it is legacy plaintext.
     *
     * <p>Returns {@code null} when an encrypted value cannot be decrypted: a key that was invalidated
     * (the user removed the screen lock on some devices, or the app was restored to a new device)
     * must read as "no token" so the caller signs the account out cleanly, not as garbage that would
     * be handed to the auth library.
     */
    @Nullable
    public static String decrypt(@Nullable String stored) {
        if (stored == null || stored.isEmpty()) return stored;
        if (!isEncrypted(stored)) return stored;
        try {
            byte[] packed = Base64.decode(stored.substring(PREFIX.length()), Base64.NO_WRAP);
            if (packed.length <= GCM_IV_BYTES) return null;
            byte[] iv = new byte[GCM_IV_BYTES];
            System.arraycopy(packed, 0, iv, 0, GCM_IV_BYTES);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] plain = cipher.doFinal(packed, GCM_IV_BYTES, packed.length - GCM_IV_BYTES);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Throwable t) {
            AppLog.warn(TAG, "decrypt failed; treating the value as unavailable", t);
            return null;
        }
    }

    /** True when the value is one this class produced. */
    public static boolean isEncrypted(@Nullable String value) {
        return value != null && value.startsWith(PREFIX);
    }

    /**
     * Removes the vault key. Called by the sign-out path so a device that is handed on does not keep
     * a key that could decrypt a backup of the account file.
     */
    public static void deleteKey() {
        try {
            KeyStore ks = KeyStore.getInstance(KEYSTORE);
            ks.load(null);
            if (ks.containsAlias(KEY_ALIAS)) ks.deleteEntry(KEY_ALIAS);
        } catch (Throwable t) {
            AppLog.warn(TAG, "could not delete the vault key", t);
        }
    }

    /** True when a vault key exists, so a caller can tell "no key yet" from "key invalidated". */
    public static boolean hasKey() {
        try {
            KeyStore ks = KeyStore.getInstance(KEYSTORE);
            ks.load(null);
            return ks.containsAlias(KEY_ALIAS);
        } catch (Throwable t) {
            return false;
        }
    }

    private static SecretKey getOrCreateKey() throws Exception {
        KeyStore ks = KeyStore.getInstance(KEYSTORE);
        ks.load(null);
        java.security.Key existing = ks.getKey(KEY_ALIAS, null);
        if (existing instanceof SecretKey) return (SecretKey) existing;

        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE);
        KeyGenParameterSpec.Builder spec = new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256);
        if (org.chimeramc.client.util.ApiLevels.atLeastN()) {
            // A key that survives a lock-screen change is the difference between an account that
            // stays signed in and one that silently signs out when the user updates their PIN.
            spec.setInvalidatedByBiometricEnrollment(false);
        }
        generator.init(spec.build());
        return generator.generateKey();
    }

    /**
     * True when this device can actually keep a Keystore key. Used by Diagnostics so a user whose
     * device reports an unusual provider sees why their account is not encrypted, instead of
     * assuming the feature is broken.
     */
    public static boolean isAvailable(Context context) {
        try {
            KeyStore ks = KeyStore.getInstance(KEYSTORE);
            ks.load(null);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
