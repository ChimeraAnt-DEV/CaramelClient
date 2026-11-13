package org.chimeramc.client.core.nethernet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** An in-memory storage that records what was written, so a test can assert no plaintext leaks. */
private class MemoryStorage : TokenStorage {
    var blob: ByteArray? = null
    var cleared = false
    override fun read(): ByteArray? = blob
    override fun write(b: ByteArray) { blob = b.copyOf(); cleared = false }
    override fun clear() { blob = null; cleared = true }
}

/** A reversible "cipher" so the tests do not need an Android Keystore. Not a cipher, a marker. */
private class XorBox(private val key: Byte = 0x5A) : SecretBox {
    override fun seal(plaintext: ByteArray) = ByteArray(plaintext.size) { (plaintext[it].toInt() xor key.toInt()).toByte() }
    override fun open(ciphertext: ByteArray) = ByteArray(ciphertext.size) { (ciphertext[it].toInt() xor key.toInt()).toByte() }
}

/**
 * The token store's two rules: nothing plaintext reaches storage, and the five-minute freshness
 * boundary is exact.
 *
 * The storage double keeps the bytes it was handed, so the "no plaintext" test checks the actual
 * blob rather than trusting the store's own claim.
 */
class TokenStoreTest {

    private var clock = 1_000_000L
    private val storage = MemoryStorage()
    private val store = TokenStore(XorBox(), storage) { clock }

    private fun snapshot(expiresInMs: Long = 3_600_000L) = TokenSnapshot(
        msAccess = "SECRET-ms-access",
        msRefresh = "SECRET-ms-refresh",
        msExpiresAtMs = clock + expiresInMs,
        xblToken = "SECRET-xbl",
        xstsToken = "SECRET-xsts",
        mcToken = "SECRET-mc-token",
        mcExpiresAtMs = clock + expiresInMs,
        xuid = "2535400000000000",
        gamertag = "SECRET-gamertag",
    )

    @Test
    fun `a snapshot round-trips through the store`() {
        store.save(snapshot())
        val loaded = store.load()
        assertEquals("SECRET-ms-access", loaded.msAccess)
        assertEquals("SECRET-ms-refresh", loaded.msRefresh)
        assertEquals("SECRET-xsts", loaded.xstsToken)
        assertEquals("SECRET-gamertag", loaded.gamertag)
    }

    @Test
    fun `no plaintext token is present in the stored blob`() {
        store.save(snapshot())
        val blob = String(storage.blob!!, Charsets.ISO_8859_1)
        for (secret in listOf("SECRET-ms-access", "SECRET-ms-refresh", "SECRET-xbl", "SECRET-xsts", "SECRET-mc-token", "SECRET-gamertag")) {
            assertFalse("the sealed blob must not contain '$secret'", blob.contains(secret))
        }
    }

    @Test
    fun `a token is stale inside the five-minute margin and fresh outside it`() {
        // Exactly 5 minutes of life: stale, because the margin consumes all of it.
        store.save(snapshot(expiresInMs = 5 * 60 * 1_000L))
        assertFalse(store.hasFreshMsToken())

        // One millisecond more than the margin: fresh.
        store.save(snapshot(expiresInMs = 5 * 60 * 1_000L + 1))
        assertTrue(store.hasFreshMsToken())
    }

    @Test
    fun `an expired token is not fresh`() {
        store.save(snapshot(expiresInMs = -1L))
        assertFalse(store.hasFreshMsToken())
        assertFalse(store.hasFreshMcToken())
    }

    @Test
    fun `clearAll removes the blob and zeroes it first`() {
        store.save(snapshot())
        store.clearAll()
        assertTrue(storage.cleared)
        assertEquals(TokenSnapshot(), store.load())
        assertFalse(store.hasFreshMsToken())
    }

    @Test
    fun `a blob that cannot be opened reads as no session`() {
        storage.write(ByteArray(16) { 0x11 })
        assertEquals(TokenSnapshot(), store.load())
        assertFalse(store.hasFreshMsToken())
    }

    @Test
    fun `a snapshot never prints a secret`() {
        val text = snapshot().toString()
        for (secret in listOf("SECRET-ms-access", "SECRET-ms-refresh", "SECRET-xbl", "SECRET-xsts", "SECRET-mc-token", "SECRET-gamertag")) {
            assertFalse("toString must redact '$secret', was '$text'", text.contains(secret))
        }
        assertTrue(text.contains("***"))
    }

    @Test
    fun `an empty store reports no fresh token`() {
        assertFalse(store.hasFreshMsToken())
        assertFalse(store.hasFreshMcToken())
        assertEquals(TokenSnapshot(), store.load())
    }

    @Test
    fun `the mc token freshness is tracked separately from the ms token`() {
        val s = snapshot().copy(mcExpiresAtMs = clock + 1_000L)
        store.save(s)
        assertTrue(store.hasFreshMsToken())
        assertFalse("a soon-to-expire MC token must not read as fresh", store.hasFreshMcToken())
    }
}
