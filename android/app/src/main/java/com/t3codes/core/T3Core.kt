package com.t3codes.core

import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.decodeFromString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import java.io.Closeable
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class T3Exception(message: String, val tag: String? = null) : RuntimeException(message)

/** Thread-safe JSON helpers shared by the JNI wrapper. */
object T3Core {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        encodeDefaults = true
    }

    fun connect(
        baseUrl: String,
        token: String,
        clientLabel: String = "T3 Code Android",
    ): T3Connection {
        val raw = Native.nativeConnect(normalizeBase(baseUrl), token.trim(), clientLabel)
        val obj = ensureOk(raw).jsonObject
        val handle = obj["handle"]?.jsonPrimitive?.long
            ?: throw T3Exception("connect: server returned no handle")
        val config = json.decodeFromJsonElement<ServerConfig>(
            obj["config"] ?: throw T3Exception("connect: server returned no config"),
        )
        return T3Connection(handle, config)
    }

    /** Redeems a pairing link or raw pairing code. Returns a bearer token. */
    fun pair(
        baseUrl: String,
        pairingLinkOrCode: String,
        clientLabel: String = "T3 Code Android",
    ): AccessToken {
        val raw = Native.nativePair(normalizeBase(baseUrl), pairingLinkOrCode.trim(), clientLabel)
        return json.decodeFromString(ensureOk(raw).jsonObject.toString())
    }

    /** Probes a server without authenticating. */
    fun probe(baseUrl: String): EnvironmentDescriptor {
        val raw = Native.nativeProbe(normalizeBase(baseUrl))
        return json.decodeFromString(ensureOk(raw).jsonObject.toString())
    }

    internal fun normalizeBase(baseUrl: String): String {
        var value = baseUrl.trim().trimEnd('/')
        if (value.isEmpty()) throw T3Exception("server URL is empty")
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            value = "http://$value"
        }
        return value
    }

    internal fun ensureOk(raw: String): JsonElement {
        val element = try {
            json.parseToJsonElement(raw)
        } catch (error: Exception) {
            throw T3Exception("invalid response from core: ${error.message}")
        }
        if (element is JsonObject) {
            val message = element["__error"]?.jsonPrimitive?.contentOrNull
            if (message != null) {
                val tag = element["__tag"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotEmpty() }
                throw T3Exception(message, tag)
            }
        }
        return element
    }
}

/** One authenticated connection to a T3 environment. */
class T3Connection internal constructor(
    private val handle: Long,
    val config: ServerConfig,
) : Closeable {
    private val closed = AtomicBoolean(false)

    private fun checkOpen() {
        if (closed.get()) throw T3Exception("connection is closed")
    }

    /** Calls any server RPC method and returns the raw JSON result. */
    fun callRaw(method: String, payload: JsonElement = JsonObject(emptyMap())): JsonElement {
        checkOpen()
        return T3Core.ensureOk(Native.nativeCall(handle, method, payload.toString()))
    }

    fun callObject(method: String, payload: JsonElement = JsonObject(emptyMap())): JsonObject =
        callRaw(method, payload).jsonObject

    fun callArray(method: String, payload: JsonElement = JsonObject(emptyMap())): JsonArray =
        callRaw(method, payload) as? JsonArray
            ?: throw T3Exception("$method: expected a JSON array")

    inline fun <reified T> call(method: String, payload: JsonElement = JsonObject(emptyMap())): T =
        T3Core.json.decodeFromJsonElement(callRaw(method, payload))

    fun configJson(): ServerConfig =
        T3Core.json.decodeFromString(Native.nativeConfig(handle).let { T3Core.ensureOk(it).toString() })

    /**
     * Re-runs provider discovery, including model catalogs, then reloads the
     * server config. The live config can start out with empty model lists, so
     * the composer asks for this before showing model choices.
     */
    fun refreshProviderCatalog(): ServerConfig {
        // Older servers may not implement refreshProviders. Still reload the
        // config afterwards so model lists already cached by the server appear.
        runCatching {
            callRaw(
                "server.refreshProviders",
                buildJsonObject { put("refreshModels", true) },
            )
        }
        return T3Core.json.decodeFromJsonElement(callObject("server.getConfig"))
    }

    fun shell(): ShellSnapshot =
        T3Core.json.decodeFromString(T3Core.ensureOk(Native.nativeShell(handle)).toString())

    fun projects(): ProjectSnapshot = call("projects.list")

    /**
     * Registers an existing folder as a project. The server may create the
     * directory if it is missing, matching the official add-project flow.
     */
    fun createProject(
        workspaceRoot: String,
        title: String = "",
        createIfMissing: Boolean = true,
    ): Project {
        val root = workspaceRoot.trim()
        val resolvedTitle = title.trim().ifBlank {
            root.split('/', '\\').lastOrNull { it.isNotBlank() } ?: "Project"
        }
        return call(
            "projects.mutate",
            buildJsonObject {
                put("type", "project.create")
                put("commandId", UUID.randomUUID().toString())
                put("projectId", UUID.randomUUID().toString())
                put("title", resolvedTitle)
                put("workspaceRoot", root)
                put("createWorkspaceRootIfMissing", createIfMissing)
            },
        )
    }

    /** Creates a new folder and an empty Git repository under the server's new-project root. */
    fun createNamedProject(name: String): ProjectCreateNewResult = call(
        "projects.createNew",
        buildJsonObject { put("name", name.trim()) },
    )

    fun browseFiles(path: String, cwd: String = ""): FilesystemBrowseResult = call(
        "filesystem.browse",
        buildJsonObject {
            put("partialPath", path.ifBlank { "~/" })
            if (cwd.isNotBlank()) put("cwd", cwd)
        },
    )

    fun discoverSourceControl(): SourceControlDiscoveryResult =
        call("server.discoverSourceControl")

    fun lookupRepository(
        provider: String,
        repository: String,
        cwd: String = "",
    ): SourceControlRepositoryInfo = call(
        "sourceControl.lookupRepository",
        buildJsonObject {
            put("provider", provider)
            put("repository", repository.trim())
            if (cwd.isNotBlank()) put("cwd", cwd)
        },
    )

    fun cloneRepository(
        remoteUrl: String = "",
        destinationPath: String,
        provider: String = "",
        repository: String = "",
    ): SourceControlCloneResult = call(
        "sourceControl.cloneRepository",
        buildJsonObject {
            if (provider.isNotBlank()) put("provider", provider)
            if (repository.isNotBlank()) put("repository", repository.trim())
            if (remoteUrl.isNotBlank()) put("remoteUrl", remoteUrl.trim())
            put("destinationPath", destinationPath.trim())
        },
    )

    fun startProjectClone(
        projectId: String,
        title: String,
        remoteUrl: String = "",
        destinationPath: String,
        provider: String = "",
        repository: String = "",
    ): ProjectCloneStartResult = call(
        "projectClone.start",
        buildJsonObject {
            put("projectId", projectId)
            put("title", title.trim())
            put("createdAt", java.time.Instant.now().toString())
            if (provider.isNotBlank()) put("provider", provider)
            if (repository.isNotBlank()) put("repository", repository.trim())
            if (remoteUrl.isNotBlank()) put("remoteUrl", remoteUrl.trim())
            put("destinationPath", destinationPath.trim())
        },
    )

    fun cancelProjectClone(projectId: String): ProjectCloneActionResult = call(
        "projectClone.cancel",
        buildJsonObject { put("projectId", projectId) },
    )

    fun retryProjectClone(projectId: String): ProjectCloneActionResult = call(
        "projectClone.retry",
        buildJsonObject { put("projectId", projectId) },
    )

    fun publishRepository(
        cwd: String,
        provider: String,
        repository: String,
        visibility: String = "private",
    ): JsonElement = callRaw(
        "sourceControl.publishRepository",
        buildJsonObject {
            put("cwd", cwd.trim())
            put("provider", provider)
            put("repository", repository.trim())
            put("visibility", visibility)
        },
    )

    fun watchShell(): ShellFeed = ShellFeed(Native.nativeShellWatch(handle).handleOrThrow("subscribeShell"))

    fun thread(threadId: String): ThreadDetailSnapshot =
        T3Core.json.decodeFromString(
            T3Core.ensureOk(Native.nativeThread(handle, threadId)).toString(),
        )

    fun threadHistory(threadId: String, cursor: String? = null): ThreadHistoryPage =
        T3Core.json.decodeFromString(
            T3Core.ensureOk(Native.nativeThreadHistory(handle, threadId, cursor ?: "")).toString(),
        )

    fun watchThread(
        threadId: String,
        afterSequence: Long = -1,
        acceptBounded: Boolean = false,
    ): ThreadFeed = ThreadFeed(
        Native.nativeThreadWatch(handle, threadId, afterSequence, acceptBounded)
            .handleOrThrow("subscribeThread"),
    )

    fun openStream(method: String, payload: JsonElement = JsonObject(emptyMap())): RawStream =
        RawStream(
            Native.nativeStreamOpen(handle, method, payload.toString())
                .handleOrThrow(method),
        )

    fun sendMessage(
        threadId: String,
        text: String,
        attachments: List<Attachment> = emptyList(),
        mode: String = "start_immediately",
        targetRunId: String? = null,
    ): Long {
        val attachmentsJson = buildJsonArray {
            attachments.forEach { add(T3Core.json.encodeToJsonElement(it)) }
        }.toString()
        val result = T3Core.ensureOk(
            Native.nativeSendMessage(handle, threadId, text, attachmentsJson, mode, targetRunId ?: ""),
        ).jsonObject
        return result["sequence"]?.jsonPrimitive?.long ?: 0L
    }

    fun launchThread(
        projectId: String,
        title: String,
        modelSelection: ModelSelection,
        workspace: WorkspaceStrategy = WorkspaceStrategy(),
        initialText: String? = null,
        runtimeMode: String = "full-access",
        interactionMode: String = "default",
    ): ThreadLaunchResult {
        val result = T3Core.ensureOk(
            Native.nativeLaunchThread(
                handle,
                projectId,
                title,
                T3Core.json.encodeToJsonElement(modelSelection).toString(),
                T3Core.json.encodeToJsonElement(workspace).toString(),
                initialText ?: "",
                runtimeMode,
                interactionMode,
            ),
        )
        return T3Core.json.decodeFromJsonElement(result)
    }

    fun stopThread(threadId: String) {
        T3Core.ensureOk(Native.nativeStopThread(handle, threadId))
    }

    /**
     * Interrupts one run. Unlike thread.stop, run.interrupt is supported by
     * environments that predate the thread-stop capability.
     */
    fun interruptRun(threadId: String, runId: String) {
        callRaw(
            "orchestration.dispatchCommand",
            JsonObject(
                mapOf(
                    "type" to JsonPrimitive("run.interrupt"),
                    "commandId" to JsonPrimitive(UUID.randomUUID().toString()),
                    "threadId" to JsonPrimitive(threadId),
                    "runId" to JsonPrimitive(runId),
                    "holdQueue" to JsonPrimitive(true),
                ),
            ),
        )
    }

    fun respondToRequest(
        threadId: String,
        requestId: String,
        decision: String,
        feedback: String = "",
        answers: JsonObject = JsonObject(emptyMap()),
    ) {
        T3Core.ensureOk(
            Native.nativeRespond(handle, threadId, requestId, decision, feedback, answers.toString()),
        )
    }

    fun dismissUserInput(threadId: String, requestId: String) {
        T3Core.ensureOk(Native.nativeDismissUserInput(handle, threadId, requestId))
    }

    /**
     * Thread mutation shortcuts: archive, unarchive, delete, rename, pin,
     * unpin, markRead, markUnread, settle, unsettle, snooze, unsnooze,
     * setModel, setRuntimeMode, setInteractionMode, resumeQueue.
     */
    fun threadAction(threadId: String, action: String, value: String = "") {
        T3Core.ensureOk(Native.nativeThreadAction(handle, threadId, action, value))
    }

    fun openTerminal(
        threadId: String,
        terminalId: String = "term-1",
        cwd: String,
        cols: Int = 80,
        rows: Int = 24,
    ): TerminalSession = T3Core.json.decodeFromString(
        T3Core.ensureOk(
            Native.nativeTerminalOpen(handle, threadId, terminalId, cwd, cols, rows),
        ).toString(),
    )

    fun attachTerminal(
        threadId: String,
        terminalId: String = "term-1",
        cwd: String = "",
        cols: Int = 80,
        rows: Int = 24,
    ): TerminalFeed = TerminalFeed(
        Native.nativeTerminalAttach(handle, threadId, terminalId, cwd, cols, rows)
            .handleOrThrow("terminal.attach"),
    )

    fun writeTerminal(threadId: String, terminalId: String, data: String) {
        T3Core.ensureOk(Native.nativeTerminalWrite(handle, threadId, terminalId, data))
    }

    fun resizeTerminal(threadId: String, terminalId: String, cols: Int, rows: Int) {
        T3Core.ensureOk(Native.nativeTerminalResize(handle, threadId, terminalId, cols, rows))
    }

    fun closeTerminal(threadId: String, terminalId: String) {
        T3Core.ensureOk(Native.nativeTerminalClose(handle, threadId, terminalId))
    }

    fun uploadAttachment(
        type: String,
        name: String,
        mimeType: String,
        data: ByteArray,
    ): Attachment = T3Core.json.decodeFromString(
        T3Core.ensureOk(
            Native.nativeAttachmentUpload(handle, type, name, mimeType, data),
        ).toString(),
    )

    fun usage(sinceDay: String = "", untilDay: String = "", timeZone: String = ""): UsageSummary =
        T3Core.json.decodeFromString(
            T3Core.ensureOk(Native.nativeUsage(handle, sinceDay, untilDay, timeZone)).toString(),
        )

    fun vcsStatus(cwd: String): VcsStatus =
        T3Core.json.decodeFromString(
            T3Core.ensureOk(Native.nativeVcsStatus(handle, cwd)).toString(),
        )

    fun listRefs(
        cwd: String,
        query: String = "",
        refKind: String = "",
        limit: Int = 100,
    ): ListRefsResult {
        val payload = buildJsonObject {
            put("cwd", cwd)
            if (query.isNotBlank()) put("query", query)
            if (refKind.isNotBlank()) put("refKind", refKind)
            if (limit > 0) put("limit", limit)
        }
        return call("vcs.listRefs", payload)
    }

    fun diffPreview(cwd: String): DiffPreview =
        T3Core.json.decodeFromString(
            T3Core.ensureOk(Native.nativeDiffPreview(handle, cwd)).toString(),
        )

    fun listEntries(cwd: String, directory: String = ""): ListEntriesResult =
        T3Core.json.decodeFromString(
            T3Core.ensureOk(Native.nativeListEntries(handle, cwd, directory)).toString(),
        )

    fun readFile(cwd: String, path: String): ReadFileResult =
        T3Core.json.decodeFromString(
            T3Core.ensureOk(Native.nativeReadFile(handle, cwd, path)).toString(),
        )

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            Native.nativeClose(handle)
        }
    }
}

/** Live project/thread index backed by subscribeShell. */
class ShellFeed internal constructor(private val handle: Long) : Closeable {
    private val closed = AtomicBoolean(false)

    val events: Flow<ShellEvent> = flow {
        while (!closed.get()) {
            val element = T3Core.ensureOk(Native.nativeShellNext(handle, POLL_TIMEOUT_MS))
            val obj = element as? JsonObject ?: continue
            if (obj["timeout"]?.jsonPrimitive?.booleanOrNull == true) continue
            val event = T3Core.json.decodeFromJsonElement<ShellEvent>(obj)
            emit(event)
            if (event.kind == "closed" || obj["done"]?.jsonPrimitive?.booleanOrNull == true) break
        }
    }.flowOn(Dispatchers.IO)

    fun snapshot(): ShellSnapshot =
        T3Core.json.decodeFromString(T3Core.ensureOk(Native.nativeShellState(handle)).toString())

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            Native.nativeShellClose(handle)
        }
    }
}

/** Live thread projection backed by subscribeThread. */
class ThreadFeed internal constructor(private val handle: Long) : Closeable {
    private val closed = AtomicBoolean(false)

    val events: Flow<ThreadEvent> = flow {
        while (!closed.get()) {
            val element = T3Core.ensureOk(Native.nativeThreadNext(handle, POLL_TIMEOUT_MS))
            val obj = element as? JsonObject ?: continue
            if (obj["timeout"]?.jsonPrimitive?.booleanOrNull == true) continue
            val event = T3Core.json.decodeFromJsonElement<ThreadEvent>(obj)
            emit(event)
            if (event.kind == "closed" || obj["done"]?.jsonPrimitive?.booleanOrNull == true) break
        }
    }.flowOn(Dispatchers.IO)

    fun state(): ThreadState =
        T3Core.json.decodeFromString(T3Core.ensureOk(Native.nativeThreadState(handle)).toString())

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            Native.nativeThreadClose(handle)
        }
    }
}

/** One terminal's output stream. */
class TerminalFeed internal constructor(private val handle: Long) : Closeable {
    private val closed = AtomicBoolean(false)

    val events: Flow<TerminalEvent> = flow {
        while (!closed.get()) {
            val element = T3Core.ensureOk(Native.nativeTerminalNext(handle, POLL_TIMEOUT_MS))
            val obj = element as? JsonObject ?: continue
            if (obj["timeout"]?.jsonPrimitive?.booleanOrNull == true) continue
            val event = T3Core.json.decodeFromJsonElement<TerminalEvent>(obj)
            emit(event)
            if (event.type == "closed" || obj["done"]?.jsonPrimitive?.booleanOrNull == true) break
        }
    }.flowOn(Dispatchers.IO)

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            Native.nativeTerminalCloseHandle(handle)
        }
    }
}

/** A raw server stream (for example subscribeVcsStatus or command output). */
class RawStream internal constructor(private val handle: Long) : Closeable {
    private val closed = AtomicBoolean(false)

    fun next(timeoutMs: Int = 30_000): StreamFrame =
        T3Core.json.decodeFromString(
            T3Core.ensureOk(Native.nativeStreamNext(handle, timeoutMs)).toString(),
        )

    fun frames(): Flow<StreamFrame> = flow {
        while (!closed.get()) {
            val frame = next()
            if (frame.timeout) continue
            emit(frame)
            if (frame.done) break
        }
    }.flowOn(Dispatchers.IO)

    fun items(): Flow<JsonElement> = flow {
        frames().collect { frame ->
            frame.values.forEach { emit(it) }
        }
    }.flowOn(Dispatchers.IO)

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            Native.nativeStreamClose(handle)
        }
    }
}

private const val POLL_TIMEOUT_MS = 1_000

private fun String.handleOrThrow(what: String): Long {
    val obj = T3Core.ensureOk(this).jsonObject
    return obj["handle"]?.jsonPrimitive?.long
        ?: throw T3Exception("$what: server returned no handle")
}

internal fun JsonObject.string(key: String): String? =
    this[key]?.jsonPrimitive?.contentOrNull

internal fun JsonObject.long(key: String): Long? =
    this[key]?.jsonPrimitive?.long

internal fun JsonObject.int(key: String): Int? =
    this[key]?.jsonPrimitive?.intOrNull

internal fun jsonNull(): JsonNull = JsonNull

internal fun jsonString(value: String): JsonPrimitive = JsonPrimitive(value)

internal fun jsonObject(builder: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit): JsonObject =
    buildJsonObject(builder)

internal fun jsonArray(builder: kotlinx.serialization.json.JsonArrayBuilder.() -> Unit): JsonArray =
    buildJsonArray(builder)
