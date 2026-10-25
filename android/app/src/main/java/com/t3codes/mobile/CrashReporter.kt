package com.t3codes.mobile

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

/**
 * Writes a readable crash log and hands it to the standalone crash screen.
 *
 * The crash screen lives in a separate `:crash` process so it can survive the
 * main process going away. If Android refuses to launch it from the dying
 * process, the next MainActivity start picks up the pending crash instead.
 */
object CrashReporter {
    private const val PREFS = "t3_crash_reports"
    private const val KEY_PENDING_PATH = "pending_path"
    private const val CRASH_DIR = "crashes"
    private const val MAX_REPORTS = 5

    @Volatile
    private var installed = false

    fun install(application: Application) {
        if (installed || isCrashProcess(application)) return
        installed = true
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            val file = runCatching { writeReport(application, thread, error) }.getOrNull()
            if (file != null) {
                pendingPrefs(application)
                    .edit()
                    .putString(KEY_PENDING_PATH, file.absolutePath)
                    .commit()
            }
            runCatching { launchCrashScreen(application, file) }
                .onFailure {
                    // If the crash screen cannot start, fall back to Android's
                    // default handler so the failure is not silent.
                    if (previous != null) {
                        previous.uncaughtException(thread, error)
                    }
                }
            Process.killProcess(Process.myPid())
            exitProcess(10)
        }
    }

    fun launchCrashScreen(context: Context, file: File?) {
        context.startActivity(
            Intent(context, CrashActivity::class.java).apply {
                if (file != null) putExtra(CrashActivity.EXTRA_CRASH_PATH, file.absolutePath)
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION,
                )
            },
        )
    }

    /** Returns the crash that has not been shown yet, then clears the marker. */
    fun takePending(context: Context): File? {
        val prefs = pendingPrefs(context)
        val path = prefs.getString(KEY_PENDING_PATH, null) ?: return null
        prefs.edit().remove(KEY_PENDING_PATH).commit()
        return File(path).takeIf { it.isFile && it.length() > 0 }
    }

    fun latest(context: Context): File? =
        crashDir(context)
            .listFiles()
            ?.filter { it.isFile }
            ?.maxByOrNull { it.lastModified() }

    fun share(context: Context, file: File) {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "T3 Code Android crash report")
            putExtra(Intent.EXTRA_TEXT, "T3 Code Android crashed. The full log is attached.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, "Share crash log").apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newRawUri("T3 Code Android crash log", uri)
        }
        if (context !is android.app.Activity) {
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun copy(context: Context, file: File) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newPlainText("T3 Code Android crash report", file.readText()),
        )
        Toast.makeText(context, "Crash log copied", Toast.LENGTH_SHORT).show()
    }

    private fun writeReport(context: Context, thread: Thread, error: Throwable): File {
        val dir = crashDir(context)
        val now = Date()
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US).format(now)
        val file = File(dir, "t3code-crash-$stamp.log")
        val report = buildString {
            appendLine("T3 Code Android crash report")
            appendLine("============================")
            appendLine("Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.US).format(now)}")
            appendLine("App: ${BuildConfig.APPLICATION_ID} ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
            appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("ABI: ${Build.SUPPORTED_ABIS.joinToString(", ")}")
            appendLine("Thread: ${thread.name} (id ${thread.id})")
            appendLine()
            appendLine("Fatal exception:")
            appendLine(error.stackTraceToString())
        }
        file.writeText(report)
        prune(dir)
        return file
    }

    private fun prune(dir: File) {
        dir.listFiles()
            ?.filter { it.isFile }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(MAX_REPORTS)
            ?.forEach { runCatching { it.delete() } }
    }

    private fun crashDir(context: Context): File =
        File(context.filesDir, CRASH_DIR).apply { mkdirs() }

    private fun pendingPrefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun isCrashProcess(context: Context): Boolean {
        val processName = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Application.getProcessName()
            } else {
                File("/proc/self/cmdline").readText().trim('\u0000', '\n', ' ')
            }
        }.getOrDefault("")
        return processName == "${context.packageName}:crash"
    }
}
