package com.android.avbtoolkit

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Runs the bundled AOSP avbtool.py inside the Chaquopy (CPython)
 * runtime. This is the single entry point shared by the GUI command
 * form and the interactive console.
 *
 * Files selected through SAF are exposed to Python as `/saf/fd/<fd>`
 * pseudo-paths (see android_bridge.py), avoiding copies of large images.
 */
object AvbExecutor {

    @Volatile
    private var appContext: Context? = null

    private val nativeLibDir: String
        get() = appContext!!.applicationInfo.nativeLibraryDir

    /** Initialise the Python runtime and native FEC library once. */
    fun ensureStarted(context: Context) {
        appContext = context.applicationContext
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(appContext!!))
        }
        val py = Python.getInstance()
        if (py.getModule("android_bridge").callAttr("is_initialized").toBoolean() == false) {
            py.getModule("android_bridge").callAttr("init", nativeLibDir)
        }
    }

    /**
     * Resolve a SAF uri to a /saf/fd/<fd> path (read-only). The caller
     * must release the fd with [releaseFd] afterwards.
     */
    fun safPathFor(uri: Uri): String {
        val fd = openFdRead(uri)
        return "/saf/fd/$fd"
    }

    // fdsan safety: keep the owning ParcelFileDescriptor and pass its raw
    // fd to Python. Python os.dup()s it for each open and closes only its
    // own duplicates; we release by closing the original pfd. adoptFd on
    // a still-owned fd would abort (fdsan), crashing the app.
    private val openFds = java.util.concurrent.ConcurrentHashMap<Int, ParcelFileDescriptor>()

    /** Open a SAF content uri for reading. */
    fun openFdRead(uri: Uri): Int {
        val resolver = appContext!!.contentResolver
        val pfd = resolver.openFileDescriptor(uri, "r")
            ?: throw IllegalStateException("cannot open $uri for reading")
        return register(pfd)
    }

    /** Open a SAF content uri for reading or writing (output files). */
    fun openFdWrite(uri: Uri): Int {
        val resolver = appContext!!.contentResolver
        val pfd = resolver.openFileDescriptor(uri, "rw")
            ?: throw IllegalStateException("cannot open $uri for writing")
        return register(pfd)
    }

    private fun register(pfd: ParcelFileDescriptor): Int {
        val fd = pfd.fd
        openFds[fd] = pfd
        return fd
    }

    /** Close the pfd that backs [fd]; safe to call multiple times. */
    fun releaseFd(fd: Int) {
        openFds.remove(fd)?.close()
    }

    /** A SAF fd kept open until [close] is called. */
    class AcquiredFd(uri: Uri) : AutoCloseable {
        val fd: Int = openFdRead(uri)
        val pseudoPath: String = "/saf/fd/$fd"
        override fun close() = releaseFd(fd)
    }

    data class RunResult(val exitCode: Int, val stdout: String, val stderr: String) {
        val ok: Boolean get() = exitCode == 0
    }

    /**
     * Run `avbtool <argv>` and collect stdout/stderr. Returns a result
     * object; throws [CancellationException] on thread cancellation.
     */
    suspend fun run(argv: List<String>): RunResult = withContext(Dispatchers.IO) {
        val py = Python.getInstance()
        val bridge = py.getModule("android_bridge")
        val result = bridge.callAttr(
            "run_avbtool",
            PyObject.fromJava(argv.toTypedArray()),
        )
        val tuple = result.asList()
        val exitCode = tuple[0].toInt()
        val stdout = tuple[1].toString()
        val stderr = tuple[2].toString()
        RunResult(exitCode, stdout, stderr)
    }
}

/** Convenience: open an SAF uri for the duration of a block. */
inline fun <T> withAcquiredFd(
    uri: Uri,
    resolver: ContentResolver,
    block: (AvbExecutor.AcquiredFd) -> T,
): T {
    val fd = AvbExecutor.AcquiredFd(uri)
    try {
        return block(fd)
    } finally {
        fd.close()
    }
}