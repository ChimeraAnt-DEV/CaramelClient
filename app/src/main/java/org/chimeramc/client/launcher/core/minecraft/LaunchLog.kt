package org.chimeramc.client.core.minecraft

import android.content.Context
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.jvm.JvmStatic

/**
 * Persistent on-disk log for the game-launch pipeline.
 *
 * Every log line that goes through [MinecraftRuntimePreparer.ProgressListener.onLog] (or
 * mirrors of `Log.w/e` during native-library loading() is appended, in real time, to
 * `context.getExternalFilesDir(null)/launch_debug.log` — a plain text file opened in append
 * mode and flushed after every write, so it survives even if the process is killed or
 * restarted before the in-memory LogcatOverlay can be viewed.

 * Thread-safe: appends are serialized through a single writer; no buffering is left
 * on disk (writer never wraps the OS buffer beyond one logical write, and flushes
 * per call(, so once this function returns, the line is durable.
 */
object LaunchLog {

    private val lock = Any()
    private var writer: PrintWriter? = null
    private var file: File? = null

    private val timestampFormat = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)



    /** Lazily opens the append-mode writer (creates the parent dir if needed). */
    private fun writer(context: Context): PrintWriter? {
        synchronized (lock) {
            if (writer != null) return writer
            return try {
                val dir = context.getExternalFilesDir(null)
                    ?: context.filesDir
                val target = File(dir, "launch_debug.log")
                target.parentFile?.mkdirs()
                val w = PrintWriter(FileWriter(target, true), true) // second boolean: autoFlush
                writer = w
                file = target
                w
            } catch (_: Throwable) {
                null
            }
        }
    }

    /**
     * Caps the log file, keeping the newest half.
     *
     * <p>Runs on the writer's own thread under the same lock every append takes, so it cannot race a
     * write. The check is a stat of the open file, so it costs nothing on the common path; the
     * truncate itself only runs once the file has passed the cap. The kept tail is what a debug
     * bundle needs: after a crash the end of the log is the interesting part, and a rotate that
     * renamed the file would leave the reader on the oldest lines.
     */
    private fun rotateIfNeeded(w: PrintWriter) {
        val target = file ?: return
        val size = try {
            target.length()
        } catch (_: Throwable) {
            return
        }
        if (!LogRotation.shouldRotate(size)) return
        val cut = try {
            java.io.RandomAccessFile(target, "rw").use { raf ->
                val rawCut = LogRotation.truncateFrom(size)
                val windowLen = (size - rawCut).coerceAtMost(WINDOW_BYTES).toInt()
                val window = ByteArray(windowLen)
                raf.seek(rawCut)
                raf.readFully(window)
                val offset = LogRotation.tailOffset(size, window)
                val tail = ByteArray((size - offset).toInt())
                raf.seek(offset)
                raf.readFully(tail)
                raf.setLength(0L)
                raf.seek(0L)
                raf.write(tail)
                offset
            }
        } catch (_: Throwable) {
            return
        }
        // Reopen the writer: it still points at the old end-of-file offset, which no longer exists.
        try {
            w.flush()
        } catch (_: Throwable) { /* best-effort: the log sink itself */ }
        try {
            val dir = target.parentFile
            dir?.mkdirs()
            writer = PrintWriter(FileWriter(target, true), true)
            this.file = target
        } catch (_: Throwable) { /* best-effort: the log sink itself */ }
        // A one-line note so a reader knows the file was trimmed rather than started there.
        try {
            val reopened = writer
            reopened?.append(timestampFormat.format(Date()))
            reopened?.append(" [log rotated; older lines dropped from offset ")
            reopened?.append(cut.toString())
            reopened?.append("]\n")
            reopened?.flush()
        } catch (_: Throwable) { /* best-effort: the log sink itself */ }
    }

    /** How many bytes from the cut are scanned for a line boundary. */
    private const val WINDOW_BYTES = 4096L

    /** Appends a plain line (no timestamp prefix( — used for raw onLog messages. */
    @JvmStatic
    fun append(context: Context, message: String) {
        synchronized (lock) {
            val w = writer(context) ?: return
            try {
                rotateIfNeeded(w)
                w.append(timestampFormat.format(Date())).append(' ').append(message).append('\n')
                w.flush()
            } catch (_: Throwable) { /* best-effort: the log sink itself */ }
        }
    }

    /** Appends a logcat-style line: `MM-DD HH:MM:SS.mmm pid tid I/Tag  : msg`. */
    @JvmStatic
    fun appendLogcat(context: Context, level: Char, tag: String, message: String) {
        synchronized (lock) {
            val w = writer(context) ?: return
            try {
                rotateIfNeeded(w)
                w.append(timestampFormat.format(Date()))
                    .append(' ')
                    .append(android.os.Process.myPid().toString())
                    .append(' ')
                    .append(android.os.Process.myTid().toString())
                    .append(' ')
                    .append(level).append('/').append(tag).append(": ").append(message).append('\n')
                w.flush()
            } catch (_: Throwable) { /* best-effort: the log sink itself */ }
        }
    }

    /** The log file itself (may be null before the first write or if external storage unavailable). */
    @JvmStatic
    fun resolveFile(context: Context): File? {
        synchronized (lock) {
            if (file != null) return file
            return writer(context)?.let { file }
        }
    }

    /** Reads the full historical file content,or null if unavailable/missing. */
    @JvmStatic
    fun readAll(context: Context): String? {
        val f = resolveFile(context) ?: return null
        return try {
            f.readText()
        } catch (_: Throwable) {
            null
        }
    }
}