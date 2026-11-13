package org.chimeramc.client.core.nethernet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The connection diagnostics buffer.
 *
 * The tests that matter are the negative ones: that a credential cannot get in, that the buffer
 * cannot grow without bound, and that it never touches the disk. A diagnostics buffer that leaks a
 * token is worse than no diagnostics buffer at all.
 */
class ConnectionDiagnosticsTest {

    @Test
    fun `entries come back oldest first with their elapsed time`() {
        val buffer = ConnectionDiagnostics()
        buffer.record("connecting", "first")
        buffer.record("connecting", "second")
        val snapshot = buffer.snapshot()
        assertEquals(2, snapshot.size)
        assertEquals("first", snapshot[0].detail)
        assertEquals("second", snapshot[1].detail)
        assertTrue(snapshot[0].atMillis <= snapshot[1].atMillis)
    }

    @Test
    fun `the buffer is bounded and drops the oldest entries`() {
        val buffer = ConnectionDiagnostics(maxEntries = 3)
        for (i in 1..5) buffer.record("connecting", "entry $i")
        val snapshot = buffer.snapshot()
        assertEquals(3, snapshot.size)
        assertEquals("entry 3", snapshot.first().detail)
        assertEquals("entry 5", snapshot.last().detail)
    }

    @Test
    fun `a credential is redacted on the way in`() {
        val buffer = ConnectionDiagnostics()
        buffer.record(
            "signing in",
            "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.payload.signature",
        )
        val text = buffer.asText()
        assertFalse("a bearer token reached the buffer", text.contains("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9"))
        assertTrue(text.contains("<redacted>"))
    }

    @Test
    fun `an over-long detail is truncated`() {
        val buffer = ConnectionDiagnostics()
        buffer.record("connecting", "x".repeat(5000))
        val entry = buffer.snapshot().single()
        assertTrue(entry.detail.length <= ConnectionDiagnostics.MAX_DETAIL_CHARS)
    }

    @Test
    fun `a failure entry is marked and carries the taxonomy reason`() {
        val buffer = ConnectionDiagnostics()
        buffer.record(NetherNetError.Timeout("connecting"))
        val entry = buffer.snapshot().single()
        assertTrue(entry.failure)
        assertEquals("connecting", entry.step)
        assertTrue(entry.detail.contains("Timed out"))
    }

    @Test
    fun `clear empties the buffer`() {
        val buffer = ConnectionDiagnostics()
        buffer.record("connecting", "x")
        buffer.clear()
        assertEquals(0, buffer.size)
        assertTrue(buffer.snapshot().isEmpty())
    }

    @Test
    fun `asText marks failures and keeps the order`() {
        val buffer = ConnectionDiagnostics()
        buffer.record("resolving", "ok")
        buffer.record(NetherNetError.Network("connecting", "unreachable"))
        val lines = buffer.asText().lines()
        assertEquals(2, lines.size)
        assertTrue(lines[0].contains("[ok]"))
        assertTrue(lines[1].contains("[FAIL]"))
    }

    @Test
    fun `the buffer never writes to disk`() {
        // The rule is the whole reason this class is in-memory; assert it structurally, so a future
        // edit that adds persistence has to delete this test rather than let it pass.
        val source = java.io.File(
            "src/main/java/org/chimeramc/client/core/nethernet/ConnectionDiagnostics.kt",
        ).takeIf { it.exists() }
            ?: java.io.File("app/src/main/java/org/chimeramc/client/core/nethernet/ConnectionDiagnostics.kt")
        assertTrue("the source file must be found", source.exists())
        val text = source.readText()
        for (forbidden in listOf("FileOutputStream", "FileWriter", "writeText", "openFileOutput")) {
            assertFalse("ConnectionDiagnostics must not persist: found $forbidden", text.contains(forbidden))
        }
    }
}
