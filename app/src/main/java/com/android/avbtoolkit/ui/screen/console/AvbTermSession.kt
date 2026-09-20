package com.android.avbtoolkit.ui.screen.console

import jackpal.androidterm.emulatorview.TermSession
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.android.avbtoolkit.AvbExecutor

/**
 * Terminal session for the AVB console: user lines are parsed into
 * avbtool argv and executed through [AvbExecutor]; stdout/stderr are
 * written back to the terminal. Local echo is handled by TermSession.
 */
class AvbTermSession(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
) : TermSession() {

    private val pending = java.lang.StringBuilder()

    init {
        // TermSession starts a reader thread that blocks on mTermIn.read()
        // and a writer thread that flushes to mTermOut. We neither pipe a
        // subprocess nor write through mTermOut (output goes through
        // write()/appendToEmulator), so both streams are neutralised with
        // dummies to keep the threads from NPE-ing.
        setTermIn(java.io.ByteArrayInputStream(ByteArray(0)))
        setTermOut(object : java.io.OutputStream() {
            override fun write(b: Int) {}
            override fun write(b: ByteArray, off: Int, len: Int) {}
        })
    }

    override fun processInput(data: ByteArray, offset: Int, count: Int) {
        // Feed the local emulator so typing echoes.
        appendToEmulator(data, offset, count)
        val text = StandardCharsets.UTF_8
            .newDecoder()
            .decode(ByteBuffer.wrap(data, offset, count))
            .toString()
        for (ch in text) {
            if (ch == '\n' || ch == '\r') {
                val line = pending.toString().trim()
                pending.setLength(0)
                if (line.isNotEmpty()) {
                    writeCrlf()
                    dispatch(line)
                }
            } else if (ch == '\b' || ch == 0x7f.toChar()) {
                if (pending.isNotEmpty()) pending.setLength(pending.length - 1)
            } else {
                pending.append(ch)
            }
        }
    }

    private fun dispatch(line: String) {
        val argv = parseCommand(line) ?: run {
            writeLine("unknown command: $line")
            return
        }
        scope.launch {
            try {
                val result = AvbExecutor.run(argv)
                if (result.stdout.isNotBlank()) writeOut(result.stdout)
                if (result.stderr.isNotBlank()) writeOut(result.stderr)
                if (result.stdout.endsWith('\n').not() && result.stderr.endsWith('\n').not()) {
                    writeCrlf()
                }
            } catch (e: Exception) {
                writeLine("error: ${e.message}")
            }
            writeOut("$ ")
        }
    }

    /** Parse a console line into argv; returns null when unrecognized. */
    fun parseCommand(line: String): List<String>? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return emptyList()
        // honor a handful of built-ins without invoking python
        val parts = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
        val first = parts.first()
        val second = parts.getOrNull(1)
        val args = when {
            first == "avbtool" -> parts.drop(1)
            first.endsWith(".py") -> parts.drop(1)
            (first == "python" || first == "python3") && second != null -> parts.drop(2)
            else -> return null
        }
        if (args.isEmpty()) return emptyList()
        return args
    }

    private fun writeOut(text: String) {
        try {
            write(text)
        } catch (_: Exception) {
        }
    }

    private fun writeCrlf() {
        try {
            write("\r\n")
        } catch (_: Exception) {
        }
    }

    private fun writeLine(text: String) {
        writeCrlf()
        writeOut(text)
        writeCrlf()
    }

    override fun onProcessExit() {
        pending.setLength(0)
    }
}