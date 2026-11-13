package org.chimeramc.client.core.nethernet

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A static guard against the one realistic way a token leaks: a log line that interpolates one.
 *
 * A runtime test can only cover the paths it happens to execute, and the paths that leak are the
 * error paths. So this walks the source instead and fails on any log or string-format call in the
 * connection package whose arguments mention a token field. It is deliberately conservative — it
 * matches on the field names rather than on values — because a false positive here is a cheap edit
 * and a false negative is a leaked credential.
 */
class NoTokenInLogsTest {

    private val tokenWords = listOf(
        "accessToken", "refreshToken", "xblToken", "xstsToken", "mcToken",
        "msAccess", "msRefresh", "access_token", "refresh_token",
        "xuid", "gamertag",
    )

    private val logCalls = listOf(
        "AppLog.", "Log.d(", "Log.i(", "Log.w(", "Log.e(", "Log.v(", "println(",
        "String.format(", "Log.d(TAG", "Log.i(TAG",
    )

    private fun sourceFiles(): List<File> {
        val roots = listOf(
            File("src/main/java/org/chimeramc/client/core/nethernet"),
            File("app/src/main/java/org/chimeramc/client/core/nethernet"),
        )
        val root = roots.firstOrNull { it.isDirectory } ?: return emptyList()
        return root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    }

    @Test
    fun `the connection sources are present to scan`() {
        assertTrue("the scan must find the nethernet sources", sourceFiles().isNotEmpty())
    }

    @Test
    fun `no log statement in the connection package interpolates a token field`() {
        val offenders = mutableListOf<String>()
        for (file in sourceFiles()) {
            file.readLines().forEachIndexed { index, line ->
                val isLog = logCalls.any { line.contains(it) }
                if (!isLog) return@forEachIndexed
                for (word in tokenWords) {
                    // The redacted toString() and the TokenSnapshot class itself are fine; a direct
                    // field read inside a log call is not.
                    if (line.contains(".$word") || line.contains("$word)")) {
                        offenders.add("${file.name}:${index + 1}: ${line.trim()}")
                    }
                }
            }
        }
        assertTrue(
            "a token field must never be interpolated into a log call:\n" +
                offenders.joinToString("\n"),
            offenders.isEmpty(),
        )
    }
}
