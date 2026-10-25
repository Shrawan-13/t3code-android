package com.t3codes.mobile

import android.content.Context
import com.t3codes.core.ServerConfig
import com.t3codes.core.ShellSnapshot
import com.t3codes.core.T3Core
import com.t3codes.core.ThreadDetailSnapshot
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.File
import java.security.MessageDigest

/**
 * On-device snapshot cache.
 *
 * The official mobile client restores the last shell and thread snapshots from
 * SQLite before opening a socket. That is what keeps the list from flashing
 * empty (or showing only the one thread that happened to update) while a new
 * connection is being established. We keep the same data in small JSON files;
 * the payloads are already the exact structures the C++/Go core hands back.
 */
data class RestoredSession(
    val environmentId: String,
    val config: ServerConfig,
    val shell: ShellSnapshot?,
)

class ClientCache(context: Context) {
    private val root = File(context.filesDir, "client-cache")

    /** Finds the newest cached environment for a base URL without knowing its id yet. */
    fun loadSession(baseUrl: String): RestoredSession? = runCatching {
        val normalized = baseUrl.trimEnd('/')
        val matches = root.listFiles()
            ?.mapNotNull { directory ->
                val configFile = File(directory, "config.json")
                if (!configFile.isFile) return@mapNotNull null
                val stored = runCatching {
                    T3Core.json.decodeFromString<CachedConfig>(configFile.readText())
                }.getOrNull() ?: return@mapNotNull null
                if (stored.baseUrl.trimEnd('/') != normalized || stored.environmentId.isBlank()) {
                    return@mapNotNull null
                }
                RestoredSession(
                    environmentId = stored.environmentId,
                    config = stored.config,
                    shell = loadShell(baseUrl, stored.environmentId),
                ) to configFile.lastModified()
            }
            ?.maxByOrNull { it.second }
        matches?.first
    }.getOrNull()

    @Serializable
    private data class CachedConfig(
        val baseUrl: String = "",
        val environmentId: String = "",
        val config: ServerConfig = ServerConfig(),
    )

    @Serializable
    private data class CachedShell(
        val baseUrl: String = "",
        val environmentId: String = "",
        val snapshot: ShellSnapshot = ShellSnapshot(),
    )

    @Serializable
    private data class CachedThread(
        val baseUrl: String = "",
        val environmentId: String = "",
        val threadId: String = "",
        val snapshot: ThreadDetailSnapshot = ThreadDetailSnapshot(),
    )

    private fun environmentDir(baseUrl: String, environmentId: String): File =
        File(root, digest("${baseUrl.trimEnd('/')}|$environmentId").take(20)).apply { mkdirs() }

    private fun threadFile(baseUrl: String, environmentId: String, threadId: String): File =
        File(environmentDir(baseUrl, environmentId), "thread-${digest(threadId).take(24)}.json")

    fun loadConfig(baseUrl: String, environmentId: String): ServerConfig? = runCatching {
        val file = File(environmentDir(baseUrl, environmentId), "config.json")
        if (!file.isFile) return null
        val stored = T3Core.json.decodeFromString<CachedConfig>(file.readText())
        stored.config.takeIf {
            stored.environmentId == environmentId &&
                (it.cwd.isNotBlank() || it.environment.environmentId.isNotBlank())
        }
    }.getOrNull()

    fun saveConfig(baseUrl: String, environmentId: String, config: ServerConfig) {
        write(
            File(environmentDir(baseUrl, environmentId), "config.json"),
            T3Core.json.encodeToString(CachedConfig(baseUrl, environmentId, config)),
        )
    }

    fun loadShell(baseUrl: String, environmentId: String): ShellSnapshot? = runCatching {
        val file = File(environmentDir(baseUrl, environmentId), "shell.json")
        if (!file.isFile) return null
        val stored = T3Core.json.decodeFromString<CachedShell>(file.readText())
        stored.snapshot.takeIf {
            stored.environmentId == environmentId &&
                (it.threads.isNotEmpty() || it.archivedThreads.isNotEmpty() || it.projects.isNotEmpty())
        }
    }.getOrNull()

    fun saveShell(baseUrl: String, environmentId: String, snapshot: ShellSnapshot) {
        write(
            File(environmentDir(baseUrl, environmentId), "shell.json"),
            T3Core.json.encodeToString(CachedShell(baseUrl, environmentId, snapshot)),
        )
    }

    fun loadThread(baseUrl: String, environmentId: String, threadId: String): ThreadDetailSnapshot? =
        runCatching {
            val file = threadFile(baseUrl, environmentId, threadId)
            if (!file.isFile) return null
            val stored = T3Core.json.decodeFromString<CachedThread>(file.readText())
            stored.snapshot.takeIf {
                stored.environmentId == environmentId && stored.threadId == threadId
            }
        }.getOrNull()

    fun saveThread(
        baseUrl: String,
        environmentId: String,
        threadId: String,
        snapshot: ThreadDetailSnapshot,
    ) {
        // Multi-megabyte projections make every subsequent open slower for no
        // benefit; the server still has the authoritative copy.
        val encoded = runCatching {
            T3Core.json.encodeToString(CachedThread(baseUrl, environmentId, threadId, snapshot))
        }.getOrNull() ?: return
        if (encoded.length > MAX_THREAD_CACHE_CHARS) return
        val dir = environmentDir(baseUrl, environmentId)
        write(threadFile(baseUrl, environmentId, threadId), encoded)
        pruneThreads(dir)
    }

    fun removeThread(baseUrl: String, environmentId: String, threadId: String) {
        runCatching { threadFile(baseUrl, environmentId, threadId).delete() }
    }

    fun clearThreads(baseUrl: String, environmentId: String) {
        runCatching {
            environmentDir(baseUrl, environmentId).listFiles()
                ?.filter { it.name.startsWith("thread-") }
                ?.forEach { it.delete() }
        }
    }

    private fun write(file: File, contents: String) {
        runCatching {
            file.parentFile?.mkdirs()
            val temporary = File(file.parentFile, "${file.name}.tmp")
            temporary.writeText(contents)
            if (file.exists()) file.delete()
            if (!temporary.renameTo(file)) {
                file.writeText(contents)
                temporary.delete()
            }
        }
    }

    private fun pruneThreads(directory: File) {
        runCatching {
            val files = directory.listFiles()
                ?.filter { it.name.startsWith("thread-") }
                ?.sortedByDescending { it.lastModified() }
                ?: return
            files.drop(MAX_THREAD_SNAPSHOTS).forEach { it.delete() }
        }
    }

    private fun digest(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }

    private companion object {
        const val MAX_THREAD_SNAPSHOTS = 60
        const val MAX_THREAD_CACHE_CHARS = 6_000_000
    }
}
