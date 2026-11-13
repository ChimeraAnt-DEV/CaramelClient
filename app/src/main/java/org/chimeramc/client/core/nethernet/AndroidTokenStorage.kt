package org.chimeramc.client.core.nethernet

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * The production [SecretBox]: AES-256-GCM under a key that lives in the Android Keystore.
 *
 * The key is generated on first use with `setUserAuthenticationRequired(false)` — the launcher has no
 * per-use biometric prompt — and is marked for GCM only. On hardware with a secure element the key
 * is created there; elsewhere the Keystore falls back to the TEE, and on a device with neither the
 * Keystore itself refuses, which surfaces as a [TokenStoreException] and a plain sign-in-again flow
 * rather than an unencrypted fallback.
 *
 * The IV is stored alongside the ciphertext: GCM requires a unique IV per encryption, and a fresh
 * random one per [seal] is what guarantees that.
 */
class KeystoreSecretBox(private val alias: String = "caramel_nethernet_tokens") : SecretBox {

    private fun keyStore(): KeyStore =
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private fun orCreateKey(): SecretKey {
        val ks = keyStore()
        (ks.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(false)
                .build(),
        )
        return generator.generateKey()
    }

    override fun seal(plaintext: ByteArray): ByteArray {
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, orCreateKey())
            val iv = cipher.iv
            val sealed = cipher.doFinal(plaintext)
            return ByteArray(1 + iv.size + sealed.size).also {
                it[0] = iv.size.toByte()
                System.arraycopy(iv, 0, it, 1, iv.size)
                System.arraycopy(sealed, 0, it, 1 + iv.size, sealed.size)
            }
        } catch (e: Exception) {
            throw TokenStoreException("could not seal the token blob", e)
        }
    }

    override fun open(ciphertext: ByteArray): ByteArray {
        try {
            val ivSize = ciphertext[0].toInt()
            val iv = ciphertext.copyOfRange(1, 1 + ivSize)
            val body = ciphertext.copyOfRange(1 + ivSize, ciphertext.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, orCreateKey(), GCMParameterSpec(128, iv))
            return cipher.doFinal(body)
        } catch (e: Exception) {
            throw TokenStoreException("could not open the token blob", e)
        }
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}

/**
 * The production [TokenStorage]: one private file in the app's own files directory.
 *
 * It is written through a temporary file and renamed, so a process death mid-write cannot leave a
 * half-written blob that the next launch reads as a corrupt session.
 */
class FileTokenStorage(context: Context, fileName: String = "nethernet_tokens.bin") : TokenStorage {

    private val file = File(context.filesDir, fileName)

    override fun read(): ByteArray? = if (file.isFile) file.readBytes() else null

    override fun write(blob: ByteArray) {
        val tmp = File(file.parentFile, "${file.name}.tmp")
        tmp.writeBytes(blob)
        if (!tmp.renameTo(file)) {
            file.writeBytes(blob)
            tmp.delete()
        }
    }

    override fun clear() {
        file.delete()
        File(file.parentFile, "${file.name}.tmp").delete()
    }
}
