package com.android.avbtoolkit.ui.screen.console

import com.android.avbtoolkit.AvbExecutor
import kotlinx.coroutines.launch
import jackpal.androidterm.emulatorview.TermSession
import java.io.ByteArrayInputStream
import java.io.OutputStream

/**
 * Terminal session for the AVB console.
 *
 * Keyboard input arrives through [write] (EmulatorView → TermSession);
 * we do line editing here and dispatch completed lines to
 * [AvbExecutor]. Output is drawn with [appendToEmulator] directly,
 * bypassing the legacy PTY writer queue (TermSession normally bridges a
 * subprocess; we run avbtool in-process, so both internal streams are
 * neutralised with dummies).
 */
class AvbTermSession(
    private val scope: kotlinx.coroutines.CoroutineScope =
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
) : TermSession() {

    private val line = StringBuilder()
    private val history = mutableListOf<String>()
    private var historyIndex = -1
    private var savedLine: String? = null
    private var escapeActive = false
    private val escapeBuffer = StringBuilder()
    private var bannerShown = false
    private var emulatorReady = false
    private var cursorPos = 0

    init {
        // TermSession normally bridges a PTY process; we run avbtool
        // in-process, so give the legacy reader/writer threads dummies.
        setTermIn(ByteArrayInputStream(ByteArray(0)))
        setTermOut(object : OutputStream() {
            override fun write(b: Int) = Unit
            override fun write(b: ByteArray, off: Int, len: Int) = Unit
        })
    }

    fun insertText(text: String) {
        if (!emulatorReady) return
        line.append(text)
        cursorPos = line.length
        appendToEmulator(text.toByteArray(), 0, text.toByteArray().size)
        notifyUpdate()
    }

    fun appendOutput(text: String) {
        val normalized = text.replace("\n", "\r\n")
        val bytes = normalized.toByteArray(Charsets.UTF_8)
        if (bytes.isNotEmpty()) {
            appendToEmulator(bytes, 0, bytes.size)
            notifyUpdate()
        }
    }

    fun writePrompt() {
        appendOutput("\r\n> ")
    }

    fun showBanner() {
        if (!bannerShown) {
            bannerShown = true
            appendOutput("AVBTool console. Type avbtool commands, e.g. version\r\n")
            writePrompt()
        }
    }

    fun clearScreen() {
        if (!emulatorReady) return
        appendToEmulator("\u001b[2J\u001b[H".toByteArray(), 0, 7)
        line.setLength(0)
        cursorPos = 0
        savedLine = null
        notifyUpdate()
        writePrompt()
    }

    override fun initializeEmulator(columns: Int, rows: Int) {
        super.initializeEmulator(columns, rows)
        emulatorReady = true
        showBanner()
    }

    override fun write(data: ByteArray, offset: Int, count: Int) {
        if (!emulatorReady) return
        for (i in offset until offset + count) {
            val b = data[i].toInt() and 0xff
            if (escapeActive) {
                handleEscape(b)
                continue
            }
            when (b) {
                0x03 -> cancelLine()
                0x0d, 0x0a -> executeLine()
                0x7f, 0x08 -> backspace()
                0x1b -> {
                    escapeActive = true
                    escapeBuffer.setLength(0)
                }
                else -> {
                    if (b >= 0x20) {
                        val ch = b.toChar()
                        if (cursorPos < line.length) {
                            line.insert(cursorPos, ch)
                            val remaining = line.substring(cursorPos)
                            appendToEmulator(remaining.toByteArray(), 0, remaining.length)
                            val backspaces = remaining.length - 1
                            if (backspaces > 0) {
                                appendToEmulator(
                                    ("\u001b[D".repeat(backspaces)).toByteArray(), 0, backspaces * 3
                                )
                            }
                        } else {
                            line.append(ch)
                            appendToEmulator(byteArrayOf(ch.code.toByte()), 0, 1)
                        }
                        cursorPos++
                        notifyUpdate()
                    }
                }
            }
        }
    }

    private fun handleEscape(b: Int) {
        escapeBuffer.append(b.toChar())
        val s = escapeBuffer.toString()
        when {
            s.endsWith("A") -> navigateHistory(-1)
            s.endsWith("B") -> navigateHistory(1)
            s.endsWith("C") -> moveCursor(1)
            s.endsWith("D") -> moveCursor(-1)
            s.endsWith("H") -> moveCursor(Int.MIN_VALUE)
            s.endsWith("F") -> moveCursor(Int.MAX_VALUE)
        }
        if (b.toChar() in 'A'..'Z' || b.toChar() in 'a'..'z' || b.toChar() == '~') {
            escapeActive = false
            escapeBuffer.setLength(0)
        }
    }

    private fun backspace() {
        if (cursorPos <= 0) return
        if (cursorPos < line.length) {
            line.deleteCharAt(cursorPos - 1)
            val remaining = line.substring(cursorPos - 1)
            appendToEmulator("\u001b[D".toByteArray(), 0, 3)
            appendToEmulator(remaining.toByteArray(), 0, remaining.length)
            appendToEmulator(" \u001b[D".toByteArray(), 0, 4)
            appendToEmulator(("\u001b[D".repeat(remaining.length)).toByteArray(), 0, remaining.length * 3)
            cursorPos--
        } else {
            line.deleteCharAt(cursorPos - 1)
            cursorPos--
            appendToEmulator("\b \b".toByteArray(), 0, 3)
        }
        notifyUpdate()
    }

    private fun cancelLine() {
        val len = line.length
        if (len > 0) {
            appendToEmulator("\u001b[2K\r> ".toByteArray(), 0, 9)
            line.setLength(0)
            cursorPos = 0
        }
        notifyUpdate()
    }

    private fun moveCursor(delta: Int) {
        val target = when (delta) {
            Int.MIN_VALUE -> 0
            Int.MAX_VALUE -> line.length
            else -> (cursorPos + delta).coerceIn(0, line.length)
        }
        val steps = target - cursorPos
        cursorPos = target
        if (steps != 0) {
            val seq = if (steps > 0) "\u001b[C".repeat(steps) else "\u001b[D".repeat(-steps)
            appendToEmulator(seq.toByteArray(), 0, seq.length)
        }
    }

    private fun navigateHistory(direction: Int) {
        if (history.isEmpty()) return
        if (historyIndex == -1) savedLine = line.toString()
        val newIndex = historyIndex + direction
        if (newIndex !in 0 until history.size) return
        historyIndex = newIndex
        val value = history[newIndex]
        appendToEmulator("\u001b[2K\r> ".toByteArray(), 0, 9)
        line.setLength(0)
        line.append(value)
        cursorPos = line.length
        appendToEmulator(value.toByteArray(), 0, value.length)
        notifyUpdate()
    }

    private fun executeLine() {
        val cmd = line.toString().trim()
        line.setLength(0)
        cursorPos = 0
        historyIndex = -1
        savedLine = null
        appendToEmulator("\r\n".toByteArray(), 0, 2)
        if (cmd.isNotEmpty()) {
            history.add(cmd)
            if (history.size > 50) history.removeAt(0)
            dispatch(cmd)
        } else {
            writePrompt()
        }
    }

    private fun dispatch(cmd: String) {
        val argv = parseCommand(cmd) ?: run {
            appendOutput("unknown command: $cmd\r\n")
            writePrompt()
            return
        }
        scope.launch {
            try {
                val result = AvbExecutor.run(argv)
                if (result.stdout.isNotBlank()) appendOutput(result.stdout)
                if (result.stderr.isNotBlank()) appendOutput(result.stderr)
            } catch (e: Exception) {
                appendOutput("error: ${e.message}\r\n")
            }
            writePrompt()
        }
    }

    /** Parse a console line into avbtool argv; null when unrecognized. */
    fun parseCommand(line: String): List<String>? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return emptyList()
        val parts = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
        val first = parts.first()
        val second = parts.getOrNull(1)
        val args = when {
            first == "avbtool" -> parts.drop(1)
            first.endsWith(".py") -> parts.drop(1)
            (first == "python" || first == "python3") && second != null -> parts.drop(2)
            else -> return null
        }
        return args
    }

    override fun onProcessExit() {
        line.setLength(0)
    }
}