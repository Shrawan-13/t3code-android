package com.t3codes.mobile

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.t3codes.core.Attachment
import com.t3codes.core.DiffPreview
import com.t3codes.core.Model
import com.t3codes.core.ModelSelection
import com.t3codes.core.Project
import com.t3codes.core.ProjectEntry
import com.t3codes.core.RuntimeRequest
import com.t3codes.core.ServerConfig
import com.t3codes.core.ShellFeed
import com.t3codes.core.ShellSnapshot
import com.t3codes.core.T3Connection
import com.t3codes.core.T3Core
import com.t3codes.core.T3Exception
import com.t3codes.core.TerminalFeed
import com.t3codes.core.ThreadFeed
import com.t3codes.core.ThreadProjection
import com.t3codes.core.ThreadShell
import com.t3codes.core.UsageSummary
import com.t3codes.core.VisibleTurnItem
import com.t3codes.core.WorkspaceStrategy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId

enum class AppTab(val label: String) {
    Threads("Threads"),
    Projects("Projects"),
    Usage("Usage"),
    Settings("Settings"),
}

data class ThreadUi(
    val threadId: String,
    val projection: ThreadProjection = ThreadProjection(),
    val summary: ThreadShell? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val sending: Boolean = false,
    val uploading: Boolean = false,
    val pendingAttachments: List<Attachment> = emptyList(),
    val olderItems: List<VisibleTurnItem> = emptyList(),
    val hasMoreHistory: Boolean = false,
    val historyCursor: String? = null,
    val loadingEarlier: Boolean = false,
)

data class UsageUi(
    val days: Int = 30,
    val summary: UsageSummary? = null,
    val loading: Boolean = false,
    val error: String? = null,
)

data class TerminalUi(
    val threadId: String,
    val cwd: String,
    val terminalId: String = "term-1",
    val text: String = "",
    val connected: Boolean = false,
    val error: String? = null,
)

data class DiffUi(
    val threadId: String,
    val cwd: String,
    val preview: DiffPreview? = null,
    val sourceIndex: Int = 0,
    val loading: Boolean = true,
    val error: String? = null,
)

data class FileView(
    val path: String,
    val relativePath: String,
    val contents: String,
    val byteLength: Int,
    val truncated: Boolean,
)

data class FilesUi(
    val threadId: String,
    val cwd: String,
    val directory: String = "",
    val entries: List<ProjectEntry> = emptyList(),
    val truncated: Boolean = false,
    val loading: Boolean = true,
    val error: String? = null,
    val file: FileView? = null,
)

data class UiState(
    val baseUrl: String = "http://127.0.0.1:3773",
    val token: String = "",
    val pairingLink: String = "",
    val connecting: Boolean = false,
    val error: String? = null,
    val toast: String? = null,
    val config: ServerConfig? = null,
    val projects: List<Project> = emptyList(),
    val threads: List<ThreadShell> = emptyList(),
    val archivedThreads: List<ThreadShell> = emptyList(),
    val tab: AppTab = AppTab.Threads,
    val projectFilter: String? = null,
    val search: String = "",
    val showArchived: Boolean = false,
    val activeThreadId: String? = null,
    val thread: ThreadUi? = null,
    val newThreadOpen: Boolean = false,
    val newThreadProjectId: String? = null,
    val launching: Boolean = false,
    val usage: UsageUi? = null,
    val terminal: TerminalUi? = null,
    val diff: DiffUi? = null,
    val files: FilesUi? = null,
    val themePref: ThemePref = ThemePref.System,
    val selfTestBusy: Boolean = false,
    val selfTestLog: List<String> = emptyList(),
) {
    val connected: Boolean get() = config != null

    fun projectFor(thread: ThreadShell?): Project? =
        thread?.let { t -> projects.firstOrNull { it.id == t.projectId } }

    fun summaryFor(threadId: String): ThreadShell? =
        threads.firstOrNull { it.id == threadId }
            ?: archivedThreads.firstOrNull { it.id == threadId }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("t3_connection", Context.MODE_PRIVATE)

    @Volatile
    private var connection: T3Connection? = null
    private var shellFeed: ShellFeed? = null
    private var shellJob: Job? = null
    private var threadFeed: ThreadFeed? = null
    private var threadJob: Job? = null
    private var terminalFeed: TerminalFeed? = null
    private var terminalJob: Job? = null
    private var diffJob: Job? = null
    private var filesJob: Job? = null
    private var usageJob: Job? = null

    var state by mutableStateOf(
        UiState(
            baseUrl = prefs.getString(KEY_BASE, "http://127.0.0.1:3773") ?: "http://127.0.0.1:3773",
            token = prefs.getString(KEY_TOKEN, "") ?: "",
            themePref = runCatching {
                ThemePref.valueOf(prefs.getString(KEY_THEME, ThemePref.System.name) ?: ThemePref.System.name)
            }.getOrDefault(ThemePref.System),
        ),
    )
        private set

    init {
        val shouldAutoConnect = prefs.getBoolean(KEY_AUTO, true)
        if (shouldAutoConnect && state.token.isNotBlank()) connect()
    }

    // ---------------------------------------------------------------- connection

    fun onBaseUrlChange(value: String) {
        state = state.copy(baseUrl = value)
    }

    fun onTokenChange(value: String) {
        state = state.copy(token = value)
    }

    fun onPairingLinkChange(value: String) {
        state = state.copy(pairingLink = value)
    }

    fun clearError() {
        if (state.error != null) state = state.copy(error = null)
    }

    fun consumeToast() {
        if (state.toast != null) state = state.copy(toast = null)
    }

    private fun toast(message: String) {
        state = state.copy(toast = message)
    }

    fun connect() {
        if (state.connecting) return
        val base = state.baseUrl.trim()
        val token = state.token.trim()
        if (base.isEmpty()) {
            state = state.copy(error = "Enter the T3 server URL.")
            return
        }
        if (token.isEmpty()) {
            state = state.copy(error = "Paste a bearer token or pair this device first.")
            return
        }
        state = state.copy(connecting = true, error = null)
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    stopAllStreams()
                    val next = T3Core.connect(base, token)
                    val shell = next.shell()
                    next to shell
                }
                connection = result.first
                prefs.edit()
                    .putString(KEY_BASE, base)
                    .putString(KEY_TOKEN, token)
                    .putBoolean(KEY_AUTO, true)
                    .apply()
                state = state.copy(
                    connecting = false,
                    baseUrl = base,
                    token = token,
                    config = result.first.config,
                    projects = result.second.projects,
                    threads = result.second.threads,
                    archivedThreads = result.second.archivedThreads,
                    projectFilter = null,
                    search = "",
                    error = null,
                )
                startShellWatch()
            } catch (error: Throwable) {
                connection = null
                state = state.copy(
                    connecting = false,
                    config = null,
                    projects = emptyList(),
                    threads = emptyList(),
                    archivedThreads = emptyList(),
                    error = describe(error),
                )
            }
        }
    }

    fun pair() {
        if (state.connecting) return
        val base = state.baseUrl.trim()
        val link = state.pairingLink.trim()
        if (link.isEmpty()) {
            state = state.copy(error = "Paste a pairing link or pairing code.")
            return
        }
        state = state.copy(connecting = true, error = null)
        viewModelScope.launch {
            try {
                val token = withContext(Dispatchers.IO) { T3Core.pair(base, link).accessToken }
                prefs.edit().putString(KEY_BASE, base).putString(KEY_TOKEN, token).putBoolean(KEY_AUTO, true).apply()
                state = state.copy(connecting = false, token = token, pairingLink = "", error = null)
                connect()
            } catch (error: Throwable) {
                state = state.copy(connecting = false, error = describe(error))
            }
        }
    }

    fun disconnect() {
        prefs.edit().putBoolean(KEY_AUTO, false).apply()
        viewModelScope.launch {
            withContext(Dispatchers.IO) { stopAllStreams() }
            state = state.copy(
                config = null,
                projects = emptyList(),
                threads = emptyList(),
                archivedThreads = emptyList(),
                activeThreadId = null,
                thread = null,
                terminal = null,
                diff = null,
                files = null,
                usage = null,
                newThreadOpen = false,
                tab = AppTab.Threads,
                projectFilter = null,
                search = "",
                showArchived = false,
                error = null,
            )
        }
    }

    private fun startShellWatch() {
        shellJob?.cancel()
        shellFeed?.close()
        shellJob = viewModelScope.launch {
            val conn = connection ?: return@launch
            val feed = try {
                withContext(Dispatchers.IO) { conn.watchShell() }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                toast("Live updates unavailable: ${describe(error)}")
                return@launch
            }
            shellFeed = feed
            try {
                feed.events.collect {
                    val snapshot = withContext(Dispatchers.IO) { feed.snapshot() }
                    applyShell(snapshot)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                if (state.config != null) toast("Live updates stopped: ${describe(error)}")
            } finally {
                runCatching { feed.close() }
            }
        }
    }

    private fun applyShell(snapshot: ShellSnapshot) {
        if (state.config == null) return
        val activeSummary = state.activeThreadId?.let { id ->
            snapshot.threads.firstOrNull { it.id == id }
                ?: snapshot.archivedThreads.firstOrNull { it.id == id }
        }
        state = state.copy(
            projects = snapshot.projects,
            threads = snapshot.threads,
            archivedThreads = snapshot.archivedThreads,
            thread = state.thread?.let { current ->
                if (activeSummary != null) current.copy(summary = activeSummary) else current
            },
        )
    }

    fun refresh() {
        viewModelScope.launch {
            val conn = connection ?: return@launch
            try {
                val snapshot = withContext(Dispatchers.IO) { conn.shell() }
                applyShell(snapshot)
            } catch (error: Throwable) {
                toast(describe(error))
            }
        }
    }

    // ------------------------------------------------------------------ navigation

    fun selectTab(tab: AppTab) {
        state = state.copy(tab = tab)
        if (tab == AppTab.Usage) loadUsage(state.usage?.days ?: 30)
    }

    fun setProjectFilter(projectId: String?) {
        state = state.copy(projectFilter = projectId)
    }

    fun setSearch(value: String) {
        state = state.copy(search = value)
    }

    fun toggleArchived() {
        state = state.copy(showArchived = !state.showArchived)
    }

    fun setThemePref(pref: ThemePref) {
        prefs.edit().putString(KEY_THEME, pref.name).apply()
        state = state.copy(themePref = pref)
    }

    // -------------------------------------------------------------------- threads

    fun openThread(threadId: String) {
        if (state.activeThreadId == threadId) return
        closeThread()
        val summary = state.summaryFor(threadId)
        state = state.copy(
            activeThreadId = threadId,
            thread = ThreadUi(threadId = threadId, summary = summary, loading = true),
        )
        threadJob = viewModelScope.launch {
            val conn = connection ?: return@launch
            try {
                val detail = withContext(Dispatchers.IO) { conn.thread(threadId) }
                if (!isActive || state.activeThreadId != threadId) return@launch
                state = state.copy(
                    thread = ThreadUi(
                        threadId = threadId,
                        projection = detail.projection,
                        summary = state.summaryFor(threadId) ?: summary,
                        loading = false,
                        hasMoreHistory = detail.hasMoreHistory == true,
                        historyCursor = detail.historyCursor,
                    ),
                )
                runCatching {
                    withContext(Dispatchers.IO) { conn.threadAction(threadId, "markRead") }
                }
                val feed = withContext(Dispatchers.IO) {
                    conn.watchThread(threadId, detail.snapshotSequence, false)
                }
                threadFeed = feed
                feed.events.collect {
                    val snapshot = withContext(Dispatchers.IO) {
                        runCatching { feed.state() }.getOrNull()
                    } ?: return@collect
                    if (state.activeThreadId != threadId) return@collect
                    val current = state.thread ?: return@collect
                    state = state.copy(
                        thread = current.copy(
                            projection = snapshot.projection,
                            summary = state.summaryFor(threadId) ?: current.summary,
                            loading = false,
                            error = null,
                            hasMoreHistory = current.hasMoreHistory || snapshot.hasMoreHistory,
                            historyCursor = snapshot.historyCursor ?: current.historyCursor,
                        ),
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                if (state.activeThreadId == threadId) {
                    state = state.copy(
                        thread = state.thread?.copy(loading = false, error = describe(error)),
                    )
                }
            } finally {
                runCatching { threadFeed?.close() }
                threadFeed = null
            }
        }
    }

    fun closeThread() {
        threadJob?.cancel()
        threadJob = null
        threadFeed?.close()
        threadFeed = null
        state = state.copy(activeThreadId = null, thread = null)
    }

    fun retryThread() {
        val id = state.activeThreadId ?: return
        closeThread()
        openThread(id)
    }

    fun loadEarlier() {
        val current = state.thread ?: return
        val cursor = current.historyCursor
        if (current.loadingEarlier || !current.hasMoreHistory || cursor.isNullOrEmpty()) return
        val conn = connection ?: return
        state = state.copy(thread = current.copy(loadingEarlier = true))
        viewModelScope.launch {
            try {
                val page = withContext(Dispatchers.IO) { conn.threadHistory(current.threadId, cursor) }
                val latest = state.thread ?: return@launch
                if (latest.threadId != current.threadId) return@launch
                val visibleIds = latest.projection.visibleTurnItems.map { it.item.id }.toSet()
                val merged = (page.items + latest.olderItems)
                    .distinctBy { it.item.id }
                    .filterNot { it.item.id in visibleIds }
                state = state.copy(
                    thread = latest.copy(
                        olderItems = merged,
                        hasMoreHistory = page.hasMoreHistory,
                        historyCursor = page.nextCursor,
                        loadingEarlier = false,
                    ),
                )
            } catch (error: Throwable) {
                val latest = state.thread ?: return@launch
                state = state.copy(thread = latest.copy(loadingEarlier = false, error = describe(error)))
            }
        }
    }

    fun sendMessage(text: String) {
        val current = state.thread ?: return
        if (current.sending) return
        val conn = connection ?: return
        val trimmed = text.trim()
        if (trimmed.isEmpty() && current.pendingAttachments.isEmpty()) return
        val running = current.projection.thread.activeRunId != null ||
            current.summary?.activeRunId != null ||
            current.projection.thread.status == "running"
        state = state.copy(thread = current.copy(sending = true, error = null))
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    conn.sendMessage(
                        threadId = current.threadId,
                        text = trimmed,
                        attachments = current.pendingAttachments,
                        mode = if (running) "queue_after_active" else "start_immediately",
                    )
                }
                state = state.copy(
                    thread = state.thread?.copy(
                        sending = false,
                        pendingAttachments = emptyList(),
                    ),
                )
            } catch (error: Throwable) {
                state = state.copy(
                    thread = state.thread?.copy(sending = false, error = describe(error)),
                )
            }
        }
    }

    fun stopRun() {
        val current = state.thread ?: return
        val conn = connection ?: return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { conn.stopThread(current.threadId) }
                toast("Stopping…")
            } catch (error: Throwable) {
                toast(describe(error))
            }
        }
    }

    fun respond(decision: String, feedback: String = "") {
        val current = state.thread ?: return
        val request: RuntimeRequest = current.projection.pendingRequests.firstOrNull() ?: return
        val conn = connection ?: return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    conn.respondToRequest(current.threadId, request.id, decision, feedback)
                }
                toast(if (decision == "decline") "Declined" else "Approved")
            } catch (error: Throwable) {
                toast(describe(error))
            }
        }
    }

    fun threadAction(threadId: String, action: String, value: String = "") {
        val conn = connection ?: return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { conn.threadAction(threadId, action, value) }
                when (action) {
                    "archive" -> {
                        if (state.activeThreadId == threadId) closeThread()
                        toast("Thread archived")
                    }
                    "unarchive" -> toast("Thread restored")
                    "delete" -> {
                        if (state.activeThreadId == threadId) closeThread()
                        toast("Thread deleted")
                    }
                    "rename" -> toast("Thread renamed")
                    "pin" -> toast("Thread pinned")
                    "unpin" -> toast("Thread unpinned")
                    "markRead", "markUnread", "settle", "unsettle" -> Unit
                }
                refresh()
            } catch (error: Throwable) {
                toast(describe(error))
            }
        }
    }

    fun attach(uri: Uri) {
        val current = state.thread ?: return
        val conn = connection ?: return
        if (current.uploading) return
        state = state.copy(thread = current.copy(uploading = true, error = null))
        viewModelScope.launch {
            try {
                val attachment = withContext(Dispatchers.IO) {
                    val resolver = getApplication<Application>().contentResolver
                    val name = displayName(resolver, uri)
                    val mime = resolver.getType(uri) ?: "application/octet-stream"
                    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw T3Exception("Could not read the selected file.")
                    if (bytes.size > MAX_ATTACHMENT_BYTES) {
                        throw T3Exception("Attachments are limited to ${MAX_ATTACHMENT_BYTES / (1024 * 1024)} MB.")
                    }
                    conn.uploadAttachment(
                        type = if (mime.startsWith("image/")) "image" else "file",
                        name = name,
                        mimeType = mime,
                        data = bytes,
                    )
                }
                val latest = state.thread ?: return@launch
                state = state.copy(
                    thread = latest.copy(
                        uploading = false,
                        pendingAttachments = latest.pendingAttachments + attachment,
                    ),
                )
            } catch (error: Throwable) {
                state = state.copy(
                    thread = state.thread?.copy(uploading = false, error = describe(error)),
                )
            }
        }
    }

    fun removeAttachment(attachment: Attachment) {
        val current = state.thread ?: return
        state = state.copy(
            thread = current.copy(
                pendingAttachments = current.pendingAttachments.filterNot { it.id == attachment.id },
            ),
        )
    }

    fun dismissThreadError() {
        val current = state.thread ?: return
        if (current.error != null) state = state.copy(thread = current.copy(error = null))
    }

    // ----------------------------------------------------------------- new thread

    fun openNewThread(projectId: String? = null) {
        state = state.copy(
            newThreadOpen = true,
            newThreadProjectId = projectId ?: state.projectFilter ?: state.projects.firstOrNull()?.id,
        )
    }

    fun closeNewThread() {
        state = state.copy(newThreadOpen = false)
    }

    fun setNewThreadProject(projectId: String) {
        state = state.copy(newThreadProjectId = projectId)
    }

    fun launchThread(
        projectId: String,
        title: String,
        selection: ModelSelection,
        initialText: String,
    ) {
        if (state.launching) return
        val conn = connection ?: return
        state = state.copy(launching = true, error = null)
        viewModelScope.launch {
            try {
                val resolvedTitle = title.trim().ifEmpty {
                    initialText.lineSequence().firstOrNull()?.trim()?.take(64).orEmpty().ifEmpty { "New thread" }
                }
                val result = withContext(Dispatchers.IO) {
                    conn.launchThread(
                        projectId = projectId,
                        title = resolvedTitle,
                        modelSelection = selection,
                        workspace = WorkspaceStrategy(type = "root"),
                        initialText = initialText.trim().ifEmpty { null },
                    )
                }
                state = state.copy(launching = false, newThreadOpen = false)
                refresh()
                openThread(result.threadId)
            } catch (error: Throwable) {
                state = state.copy(launching = false, error = describe(error))
            }
        }
    }

    // ---------------------------------------------------------------------- usage

    fun loadUsage(days: Int) {
        val conn = connection ?: return
        if (state.usage?.loading == true) return
        state = state.copy(usage = UsageUi(days = days, loading = true))
        usageJob?.cancel()
        usageJob = viewModelScope.launch {
            try {
                val until = LocalDate.now()
                val since = until.minusDays((days - 1).coerceAtLeast(0).toLong())
                val summary = withContext(Dispatchers.IO) {
                    conn.usage(
                        sinceDay = since.toString(),
                        untilDay = until.toString(),
                        timeZone = ZoneId.systemDefault().id,
                    )
                }
                state = state.copy(usage = UsageUi(days = days, summary = summary, loading = false))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                state = state.copy(usage = UsageUi(days = days, loading = false, error = describe(error)))
            }
        }
    }

    // ------------------------------------------------------------------- terminal

    fun openTerminal(threadId: String) {
        val conn = connection ?: return
        val cwd = resolveCwd(threadId) ?: run {
            toast("This thread has no workspace path.")
            return
        }
        closeTerminal()
        state = state.copy(terminal = TerminalUi(threadId = threadId, cwd = cwd))
        terminalJob = viewModelScope.launch {
            try {
                val feed = withContext(Dispatchers.IO) {
                    runCatching { conn.openTerminal(threadId = threadId, cwd = cwd, cols = 80, rows = 24) }
                    conn.attachTerminal(threadId = threadId, cwd = cwd, cols = 80, rows = 24)
                }
                terminalFeed = feed
                state = state.copy(terminal = state.terminal?.copy(connected = true))
                feed.events.collect { event ->
                    val terminal = state.terminal ?: return@collect
                    when (event.type) {
                        "output" -> appendTerminal(terminal, stripAnsi(event.data))
                        "snapshot" -> event.snapshot?.history?.let {
                            state = state.copy(
                                terminal = terminal.copy(text = stripAnsi(it).takeLast(MAX_TERMINAL_CHARS)),
                            )
                        }
                        "exit" -> appendTerminal(
                            terminal,
                            buildString {
                                append("\n[process exited")
                                event.exitCode?.let { append(" with code $it") }
                                append("]\n")
                            },
                        )
                        "error" -> if (event.message.isNotEmpty()) appendTerminal(terminal, "\n${event.message}\n")
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                state = state.copy(terminal = state.terminal?.copy(error = describe(error)))
            } finally {
                runCatching { terminalFeed?.close() }
                terminalFeed = null
            }
        }
    }

    private fun appendTerminal(terminal: TerminalUi, text: String) {
        if (text.isEmpty()) return
        val latest = state.terminal ?: return
        if (latest.threadId != terminal.threadId) return
        state = state.copy(
            terminal = latest.copy(text = (latest.text + text).takeLast(MAX_TERMINAL_CHARS)),
        )
    }

    fun terminalInput(text: String) {
        val terminal = state.terminal ?: return
        val conn = connection ?: return
        if (text.isEmpty()) return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    conn.writeTerminal(terminal.threadId, terminal.terminalId, "$text\r")
                }
            } catch (error: Throwable) {
                state = state.copy(terminal = state.terminal?.copy(error = describe(error)))
            }
        }
    }

    fun terminalKey(key: String) {
        val terminal = state.terminal ?: return
        val conn = connection ?: return
        val data = when (key) {
            "ctrl-c" -> "\u0003"
            "tab" -> "\t"
            "esc" -> "\u001b"
            "up" -> "\u001b[A"
            "down" -> "\u001b[B"
            "clear" -> "\u000c"
            else -> return
        }
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    conn.writeTerminal(terminal.threadId, terminal.terminalId, data)
                }
            }
        }
    }

    fun closeTerminal() {
        terminalJob?.cancel()
        terminalJob = null
        terminalFeed?.close()
        terminalFeed = null
        state = state.copy(terminal = null)
    }

    // ----------------------------------------------------------------------- diff

    fun openDiff(threadId: String) {
        val conn = connection ?: return
        val cwd = resolveCwd(threadId) ?: run {
            toast("This thread has no workspace path.")
            return
        }
        diffJob?.cancel()
        state = state.copy(diff = DiffUi(threadId = threadId, cwd = cwd, loading = true))
        diffJob = viewModelScope.launch {
            try {
                val preview = withContext(Dispatchers.IO) { conn.diffPreview(cwd) }
                state = state.copy(
                    diff = DiffUi(
                        threadId = threadId,
                        cwd = cwd,
                        preview = preview,
                        loading = false,
                    ),
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                state = state.copy(
                    diff = DiffUi(
                        threadId = threadId,
                        cwd = cwd,
                        loading = false,
                        error = describe(error),
                    ),
                )
            }
        }
    }

    fun selectDiffSource(index: Int) {
        val current = state.diff ?: return
        state = state.copy(diff = current.copy(sourceIndex = index))
    }

    fun closeDiff() {
        diffJob?.cancel()
        diffJob = null
        state = state.copy(diff = null)
    }

    // ---------------------------------------------------------------------- files

    fun openFiles(threadId: String) {
        val cwd = resolveCwd(threadId) ?: run {
            toast("This thread has no workspace path.")
            return
        }
        filesJob?.cancel()
        state = state.copy(
            files = FilesUi(threadId = threadId, cwd = cwd, directory = "", loading = true),
        )
        loadEntries(cwd, "")
    }

    private fun loadEntries(cwd: String, directory: String) {
        val conn = connection ?: return
        val current = state.files ?: return
        state = state.copy(
            files = current.copy(directory = directory, loading = true, error = null),
        )
        filesJob = viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { conn.listEntries(cwd, directory) }
                val latest = state.files ?: return@launch
                if (latest.cwd != cwd) return@launch
                state = state.copy(
                    files = latest.copy(
                        entries = result.entries,
                        truncated = result.truncated,
                        loading = false,
                    ),
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                val latest = state.files ?: return@launch
                state = state.copy(files = latest.copy(loading = false, error = describe(error)))
            }
        }
    }

    fun navigateFiles(directory: String) {
        val current = state.files ?: return
        loadEntries(current.cwd, directory)
    }

    fun openFile(path: String) {
        val conn = connection ?: return
        val current = state.files ?: return
        filesJob?.cancel()
        state = state.copy(files = current.copy(loading = true, error = null))
        filesJob = viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { conn.readFile(current.cwd, path) }
                if (result.exists == false) throw T3Exception("That file no longer exists.")
                val latest = state.files ?: return@launch
                if (latest.cwd != current.cwd) return@launch
                state = state.copy(
                    files = latest.copy(
                        loading = false,
                        error = null,
                        file = FileView(
                            path = path,
                            relativePath = result.relativePath.ifBlank { path },
                            contents = result.contents,
                            byteLength = result.byteLength,
                            truncated = result.truncated,
                        ),
                    ),
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                val latest = state.files ?: return@launch
                state = state.copy(files = latest.copy(loading = false, error = describe(error)))
            }
        }
    }

    fun refreshFiles() {
        val current = state.files ?: return
        val file = current.file
        if (file != null) openFile(file.path) else loadEntries(current.cwd, current.directory)
    }

    fun closeFile() {
        val current = state.files ?: return
        state = state.copy(files = current.copy(file = null, error = null))
    }

    fun closeFiles() {
        filesJob?.cancel()
        filesJob = null
        state = state.copy(files = null)
    }

    private fun resolveCwd(threadId: String): String? {
        val detail = state.thread?.takeIf { it.threadId == threadId }?.projection?.thread
        val summary = detail?.takeIf { it.id.isNotEmpty() } ?: state.summaryFor(threadId)
        val project = state.projectFor(summary)
        return summary?.worktreePath?.takeIf { it.isNotBlank() } ?: project?.workspaceRoot
    }

    // ---------------------------------------------------------------- diagnostics

    fun runSelfTest() {
        val conn = connection ?: run {
            toast("Connect first.")
            return
        }
        if (state.selfTestBusy) return
        state = state.copy(selfTestBusy = true, selfTestLog = emptyList())
        viewModelScope.launch {
            val lines = mutableListOf<String>()
            try {
                withContext(Dispatchers.IO) {
                    val t0 = System.currentTimeMillis()
                    val shell = conn.shell()
                    lines += "shell · ${shell.projects.size} projects, ${shell.threads.size} threads " +
                        "(${System.currentTimeMillis() - t0} ms)"

                    val feed = conn.watchShell()
                    try {
                        val first = withTimeoutOrNull(8_000) { feed.events.first() }
                        lines += first?.snapshot?.let { "subscribeShell · snapshot ${it.snapshotSequence}" }
                            ?: "subscribeShell · no snapshot within 8s"
                    } finally {
                        feed.close()
                    }

                    val thread = shell.threads.firstOrNull()
                    if (thread != null) {
                        val detail = conn.thread(thread.id)
                        lines += "thread · ${detail.projection.visibleTurnItems.size} visible items, " +
                            "${detail.projection.messages.size} messages"
                    }

                    val usage = runCatching { conn.usage() }
                    usage.onSuccess {
                        lines += "usage · ${it.buckets.size} buckets, " +
                            "%.4f USD".format(it.buckets.sumOf { bucket -> bucket.costUsd })
                    }.onFailure { lines += "usage · ${it.message}" }
                }
            } catch (error: Throwable) {
                lines += "self-test failed · ${describe(error)}"
            }
            state = state.copy(selfTestBusy = false, selfTestLog = lines.toList())
        }
    }

    // -------------------------------------------------------------------- cleanup

    private fun stopAllStreams() {
        shellJob?.cancel()
        shellJob = null
        shellFeed?.close()
        shellFeed = null
        threadJob?.cancel()
        threadJob = null
        threadFeed?.close()
        threadFeed = null
        terminalJob?.cancel()
        terminalJob = null
        terminalFeed?.close()
        terminalFeed = null
        diffJob?.cancel()
        diffJob = null
        filesJob?.cancel()
        filesJob = null
        usageJob?.cancel()
        usageJob = null
        connection?.close()
        connection = null
    }

    override fun onCleared() {
        stopAllStreams()
        super.onCleared()
    }

    private fun displayName(resolver: ContentResolver, uri: Uri): String {
        runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        val value = cursor.getString(index)
                        if (!value.isNullOrBlank()) return value
                    }
                }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "attachment"
    }

    private fun describe(error: Throwable): String = when (error) {
        is T3Exception ->
            if (error.tag.isNullOrEmpty()) error.message ?: "request failed"
            else "${error.message} (${error.tag})"
        is UnsatisfiedLinkError -> "This build only ships the arm64-v8a core (${error.message})."
        else -> error.message ?: error.javaClass.simpleName
    }

    private companion object {
        const val KEY_BASE = "base_url"
        const val KEY_TOKEN = "token"
        const val KEY_THEME = "theme_pref"
        const val KEY_AUTO = "auto_connect"
        const val MAX_ATTACHMENT_BYTES = 25 * 1024 * 1024
        const val MAX_TERMINAL_CHARS = 300_000

        val ANSI = Regex(
            "\\u001B(?:\\[[0-?]*[ -/]*[@-~]|\\][^\\u0007]*(?:\\u0007|\\u001B\\\\)|[@-_])",
        )

        fun stripAnsi(value: String): String =
            ANSI.replace(value.replace("\r\n", "\n"), "").replace('\r', '\n')
    }
}

/** Flatten provider models into selectable choices for the new-thread sheet. */
data class ModelChoice(
    val instanceId: String,
    val providerName: String,
    val model: Model,
) {
    val key: String get() = "$instanceId/${model.key}"
    val label: String get() = model.label
}

fun ServerConfig.modelChoices(): List<ModelChoice> =
    providers
        .filter { it.enabled && it.installed }
        .flatMap { provider ->
            provider.models.map { model ->
                ModelChoice(
                    instanceId = provider.instanceId,
                    providerName = provider.displayName.ifEmpty { provider.driver.ifEmpty { provider.instanceId } },
                    model = model,
                )
            }
        }
