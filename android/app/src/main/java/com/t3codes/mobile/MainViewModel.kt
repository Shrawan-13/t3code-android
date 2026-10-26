package com.t3codes.mobile

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.t3codes.core.Attachment
import com.t3codes.core.DiffPreview
import com.t3codes.core.GitRef
import com.t3codes.core.Model
import com.t3codes.core.ModelSelection
import com.t3codes.core.Project
import com.t3codes.core.ProjectEntry
import com.t3codes.core.PullRequestDetail
import com.t3codes.core.PullRequestDiffResult
import com.t3codes.core.PullRequestEntry
import com.t3codes.core.PullRequestListResult
import com.t3codes.core.PullRequestProjectError
import com.t3codes.core.PullRequestProviderSummary
import com.t3codes.core.RuntimeRequest
import com.t3codes.core.ServerConfig
import com.t3codes.core.ShellFeed
import com.t3codes.core.ShellSnapshot
import com.t3codes.core.T3Connection
import com.t3codes.core.T3Core
import com.t3codes.core.T3Exception
import com.t3codes.core.TerminalFeed
import com.t3codes.core.ThreadDetailSnapshot
import com.t3codes.core.ThreadFeed
import com.t3codes.core.ThreadProjection
import com.t3codes.core.ThreadShell
import com.t3codes.core.TurnItem
import com.t3codes.core.UsageSummary
import com.t3codes.core.VisibleTurnItem
import com.t3codes.core.WorkspaceStrategy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

enum class AppTab(val label: String) {
    Threads("Chats"),
    PullRequests("Pull requests"),
    Projects("Projects"),
    Usage("Usage"),
    Settings("Settings"),
}

val bottomTabs = listOf(AppTab.Threads, AppTab.PullRequests)

enum class ConnectionStatus {
    Connecting,
    Live,
    Reconnecting,
    Offline,
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
    val sequence: Long = 0,
    val committedSequence: Long = 0,
    val live: Boolean = false,
    val pendingUserItems: List<VisibleTurnItem> = emptyList(),
    val inputBusyRequestId: String? = null,
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

data class PullRequestsUi(
    val entries: List<PullRequestEntry> = emptyList(),
    val errors: List<PullRequestProjectError> = emptyList(),
    val providers: List<PullRequestProviderSummary> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val state: String = "open",
    val loaded: Boolean = false,
)

data class PullRequestDetailUi(
    val entry: PullRequestEntry,
    val detail: PullRequestDetail? = null,
    val diff: PullRequestDiffResult? = null,
    val loading: Boolean = true,
    val diffLoading: Boolean = false,
    val error: String? = null,
    val diffError: String? = null,
    val actionBusy: Boolean = false,
    val selectedMethod: String = "squash",
    val tab: String = "overview",
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
    val shellLoaded: Boolean = false,
    val connectionStatus: ConnectionStatus = ConnectionStatus.Offline,
    val connectionNote: String? = null,
    val modelsRefreshing: Boolean = false,
    val tab: AppTab = AppTab.Threads,
    val projectFilter: String? = null,
    val search: String = "",
    val showSettled: Boolean = true,
    val showArchived: Boolean = false,
    val homeFilterOpen: Boolean = false,
    val pullRequests: PullRequestsUi = PullRequestsUi(),
    val pullRequestDetail: PullRequestDetailUi? = null,
    val activeThreadId: String? = null,
    val thread: ThreadUi? = null,
    val newThreadOpen: Boolean = false,
    val newThreadProjectId: String? = null,
    val newThreadBranches: List<GitRef> = emptyList(),
    val newThreadBranch: String = "",
    val newThreadBranchesLoading: Boolean = false,
    val newThreadBranchesError: String? = null,
    val launching: Boolean = false,
    val usage: UsageUi? = null,
    val terminal: TerminalUi? = null,
    val diff: DiffUi? = null,
    val files: FilesUi? = null,
    val themePref: ThemePref = ThemePref.System,
    val selfTestBusy: Boolean = false,
    val selfTestLog: List<String> = emptyList(),
    val backgroundSyncEnabled: Boolean = false,
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
    private val clientCache = ClientCache(application)

    @Volatile
    private var connection: T3Connection? = null
    private var shellFeed: ShellFeed? = null
    private var shellJob: Job? = null
    private var threadFeed: ThreadFeed? = null
    private var threadJob: Job? = null
    private var threadReconcileJob: Job? = null
    private var terminalFeed: TerminalFeed? = null
    private var terminalJob: Job? = null
    private var diffJob: Job? = null
    private var filesJob: Job? = null
    private var usageJob: Job? = null
    private var pullRequestsJob: Job? = null
    private var pullRequestDetailJob: Job? = null
    private var pullRequestDiffJob: Job? = null
    private var connectJob: Job? = null
    private var shellRefreshJob: Job? = null
    private var reconnectJob: Job? = null
    private var modelsJob: Job? = null
    private var branchesJob: Job? = null
    private var reconnectAttempts = 0
    private var lastProviderRefreshAt = 0L
    private var lastShellCacheAt = 0L
    private val lastThreadCacheAt = mutableMapOf<String, Long>()
    private var cachedEnvironmentId: String? = null
    private var stoppedInBackground = false
    private var pendingNotificationThreadId: String? = null

    var state by mutableStateOf(
        UiState(
            baseUrl = prefs.getString(KEY_BASE, "http://127.0.0.1:3773") ?: "http://127.0.0.1:3773",
            token = prefs.getString(KEY_TOKEN, "") ?: "",
            themePref = runCatching {
                ThemePref.valueOf(prefs.getString(KEY_THEME, ThemePref.System.name) ?: ThemePref.System.name)
            }.getOrDefault(ThemePref.System),
            backgroundSyncEnabled = prefs.getBoolean(AppPreferences.BACKGROUND_SYNC, false),
        ),
    )
        private set

    init {
        val shouldAutoConnect = prefs.getBoolean(KEY_AUTO, true)
        if (state.token.isNotBlank()) restoreCachedSession()
        if (shouldAutoConnect && state.token.isNotBlank()) connect()
    }

    /**
     * The live socket can take seconds to come up. Restore the last shell and
     * config immediately so the home screen and the open thread render their
     * saved contents instead of a blank list while synchronization starts.
     */
    private fun restoreCachedSession() {
        val base = state.baseUrl
        viewModelScope.launch {
            val restored = withContext(Dispatchers.IO) { clientCache.loadSession(base) }
                ?: return@launch
            if (state.config != null || connection != null) return@launch
            cachedEnvironmentId = restored.environmentId
            val shell = restored.shell
            state = state.copy(
                config = restored.config,
                projects = shell?.projects ?: emptyList(),
                threads = shell?.threads ?: emptyList(),
                archivedThreads = shell?.archivedThreads ?: emptyList(),
                shellLoaded = shell != null,
                connectionStatus = ConnectionStatus.Reconnecting,
                connectionNote = "Restored saved chats. Reconnecting to ${environmentLabel()}…",
            )
        }
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

    fun showMessage(message: String) {
        toast(message)
    }

    private fun toast(message: String) {
        state = state.copy(toast = message)
    }

    fun connect(
        reconnecting: Boolean = false,
        automatic: Boolean = false,
        note: String? = null,
    ) {
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
        reconnectJob?.cancel()
        reconnectJob = null
        if (!reconnecting) reconnectAttempts = 0
        state = state.copy(
            connecting = true,
            error = null,
            connectionStatus = if (reconnecting) {
                ConnectionStatus.Reconnecting
            } else {
                ConnectionStatus.Connecting
            },
            connectionNote = note ?: if (reconnecting) {
                "Reconnecting to ${environmentLabel()}…"
            } else {
                null
            },
        )
        connectJob = viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    stopAllStreams()
                    val next = T3Core.connect(base, token)
                    try {
                        next to next.shell()
                    } catch (error: Throwable) {
                        runCatching { next.close() }
                        throw error
                    }
                }
                connection = result.first
                reconnectAttempts = 0
                prefs.edit()
                    .putString(KEY_BASE, base)
                    .putString(KEY_TOKEN, token)
                    .putBoolean(KEY_AUTO, true)
                    .apply()
                val activeThreadId = state.activeThreadId
                state = state.copy(
                    connecting = false,
                    baseUrl = base,
                    token = token,
                    config = result.first.config,
                    projects = result.second.projects,
                    threads = result.second.threads,
                    archivedThreads = result.second.archivedThreads,
                    shellLoaded = true,
                    connectionStatus = ConnectionStatus.Live,
                    connectionNote = null,
                    modelsRefreshing = false,
                    projectFilter = if (reconnecting) state.projectFilter else null,
                    search = if (reconnecting) state.search else "",
                    pullRequests = if (reconnecting) state.pullRequests else PullRequestsUi(),
                    error = null,
                )
                cachedEnvironmentId = result.first.config.environment.environmentId
                startShellWatch()
                ensureModelsLoaded(force = true, quiet = true)
                viewModelScope.launch(Dispatchers.IO) {
                    runCatching {
                        clientCache.saveConfig(base, cachedEnvironmentId.orEmpty(), result.first.config)
                        clientCache.saveShell(base, cachedEnvironmentId.orEmpty(), result.second)
                    }
                }
                // A reconnect gets a fresh native connection. Restart the open
                // thread so its feed and projection use the new handle.
                val pendingThread = pendingNotificationThreadId
                if (pendingThread != null) {
                    pendingNotificationThreadId = null
                    openThread(pendingThread)
                } else if (activeThreadId != null && state.activeThreadId == activeThreadId) {
                    state = state.copy(activeThreadId = null, thread = null)
                    openThread(activeThreadId)
                }
                startBackgroundSyncIfEnabled()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                connection = null
                if (automatic && state.config != null) {
                    val message = "Can't reach ${environmentLabel()}. Retrying…"
                    state = state.copy(
                        connecting = false,
                        connectionStatus = ConnectionStatus.Reconnecting,
                        connectionNote = message,
                        error = null,
                    )
                    scheduleReconnect(message)
                } else {
                    state = state.copy(
                        connecting = false,
                        config = null,
                        projects = emptyList(),
                        threads = emptyList(),
                        archivedThreads = emptyList(),
                        shellLoaded = false,
                        connectionStatus = ConnectionStatus.Offline,
                        connectionNote = null,
                        pullRequests = PullRequestsUi(),
                        error = describe(error),
                    )
                }
            }
        }
    }

    /** User-initiated retry from the offline/reconnecting banner. */
    fun retryConnection() {
        if (state.connecting) return
        val hasSession = state.token.isNotBlank()
        if (!hasSession) {
            state = state.copy(error = "Paste a bearer token or pair this device first.")
            return
        }
        connect(
            reconnecting = state.config != null,
            automatic = state.config != null,
            note = "Reconnecting to ${environmentLabel()}…",
        )
    }

    fun onAppBackgrounded() {
        stoppedInBackground = true
    }

    fun setBackgroundSync(enabled: Boolean) {
        prefs.edit().putBoolean(AppPreferences.BACKGROUND_SYNC, enabled).apply()
        state = state.copy(backgroundSyncEnabled = enabled)
        val application = getApplication<Application>()
        if (enabled) {
            if (state.token.isBlank()) {
                toast("Connect to T3 Code first.")
                return
            }
            runCatching { BackgroundSyncService.start(application) }
                .onFailure { toast("Could not start background sync: ${it.message}") }
        } else {
            BackgroundSyncService.stop(application)
        }
    }

    fun startBackgroundSyncIfEnabled() {
        if (!state.backgroundSyncEnabled || state.token.isBlank()) return
        runCatching { BackgroundSyncService.start(getApplication<Application>()) }
    }

    fun openThreadFromNotification(threadId: String) {
        if (threadId.isBlank()) return
        if (state.config != null && connection != null) {
            openThread(threadId)
            return
        }
        pendingNotificationThreadId = threadId
        if (state.connecting) return
        if (state.token.isBlank()) {
            pendingNotificationThreadId = null
            toast("Connect to T3 Code to open this chat.")
            return
        }
        connect()
    }

    fun onAppForegrounded() {
        startBackgroundSyncIfEnabled()
        if (!stoppedInBackground) return
        stoppedInBackground = false
        if (state.connecting) return
        if (state.token.isBlank()) return
        val conn = connection
        if (state.config == null || conn == null) {
            retryConnection()
            return
        }
        state = state.copy(
            connectionStatus = ConnectionStatus.Reconnecting,
            connectionNote = "Checking connection to ${environmentLabel()}…",
        )
        viewModelScope.launch {
            val snapshot = withContext(Dispatchers.IO) {
                runCatching { conn.shell() }.getOrNull()
            }
            if (snapshot == null) {
                if (connection === conn) {
                    connect(
                        reconnecting = true,
                        automatic = true,
                        note = "Reconnecting to ${environmentLabel()}…",
                    )
                }
                return@launch
            }
            if (connection !== conn) return@launch
            applyShell(snapshot)
            state = state.copy(
                shellLoaded = true,
                connectionStatus = ConnectionStatus.Live,
                connectionNote = null,
            )
            startShellWatch()
            ensureModelsLoaded(force = false)
            // Websocket feeds can be torn down while Android backgrounds the
            // app even though plain HTTP still works. Re-subscribe to the open
            // thread so its timeline becomes live again.
            if (state.activeThreadId != null) retryThread()
        }
    }

    private fun environmentLabel(): String =
        state.config?.environment?.label?.ifBlank { "the machine" } ?: "the machine"

    private fun scheduleReconnect(note: String? = null) {
        if (state.config == null || state.connecting) return
        if (reconnectJob?.isActive == true) return
        reconnectAttempts = (reconnectAttempts + 1).coerceAtMost(6)
        val waitMs = when (reconnectAttempts) {
            1 -> 1_000L
            2 -> 2_000L
            3 -> 4_000L
            4 -> 8_000L
            else -> 15_000L
        }
        state = state.copy(
            connectionStatus = ConnectionStatus.Reconnecting,
            connectionNote = note ?: "Reconnecting to ${environmentLabel()}…",
        )
        reconnectJob = viewModelScope.launch {
            delay(waitMs)
            reconnectJob = null
            if (state.config != null && !state.connecting) {
                connect(
                    reconnecting = true,
                    automatic = true,
                    note = "Reconnecting to ${environmentLabel()}…",
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
                if (state.backgroundSyncEnabled) BackgroundSyncService.stop(getApplication<Application>())
                state = state.copy(connecting = false, token = token, pairingLink = "", error = null)
                connect()
            } catch (error: Throwable) {
                state = state.copy(connecting = false, error = describe(error))
            }
        }
    }

    fun disconnect() {
        prefs.edit()
            .putBoolean(KEY_AUTO, false)
            .putBoolean(AppPreferences.BACKGROUND_SYNC, false)
            .apply()
        BackgroundSyncService.stop(getApplication<Application>())
        reconnectJob?.cancel()
        reconnectJob = null
        reconnectAttempts = 0
        lastProviderRefreshAt = 0L
        stoppedInBackground = false
        pendingNotificationThreadId = null
        modelsJob?.cancel()
        modelsJob = null
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
                showSettled = true,
                showArchived = false,
                homeFilterOpen = false,
                pullRequests = PullRequestsUi(),
                pullRequestDetail = null,
                shellLoaded = false,
                connectionStatus = ConnectionStatus.Offline,
                connectionNote = null,
                modelsRefreshing = false,
                error = null,
                backgroundSyncEnabled = false,
            )
        }
    }

    private fun startShellWatch() {
        shellJob?.cancel()
        shellFeed?.close()
        shellFeed = null
        shellJob = viewModelScope.launch {
            var attempts = 0
            while (isActive && state.config != null) {
                val conn = connection ?: break
                var feed: ShellFeed? = null
                try {
                    val activeFeed = withContext(Dispatchers.IO) { conn.watchShell() }
                    feed = activeFeed
                    if (connection !== conn) {
                        runCatching { activeFeed.close() }
                        break
                    }
                    shellFeed = activeFeed
                    attempts = 0
                    state = state.copy(
                        connectionStatus = ConnectionStatus.Live,
                        connectionNote = null,
                    )
                    try {
                        activeFeed.events.collect {
                            val snapshot = withContext(Dispatchers.IO) { activeFeed.snapshot() }
                            applyShell(snapshot)
                        }
                    } finally {
                        runCatching { activeFeed.close() }
                        if (shellFeed === activeFeed) shellFeed = null
                    }
                    if (!isActive || connection !== conn) break
                    state = state.copy(
                        connectionStatus = ConnectionStatus.Reconnecting,
                        connectionNote = "Live updates paused. Reconnecting…",
                    )
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Throwable) {
                    runCatching { feed?.close() }
                    if (connection !== conn) break
                    Log.w(LOG_TAG, "shell feed failed", error)
                    state = state.copy(
                        connectionStatus = ConnectionStatus.Reconnecting,
                        connectionNote = "Reconnecting to ${environmentLabel()}…",
                    )
                }
                attempts++
                if (attempts >= 3) {
                    scheduleReconnect("Live updates lost. Reconnecting to ${environmentLabel()}…")
                    break
                }
                delay(attempts * 1_000L)
            }
        }
    }

    private fun applyShell(snapshot: ShellSnapshot) {
        if (state.config == null) return
        // A successful shell read means the session is healthy again. Cancel
        // any retry that was queued while it was down.
        if (connection != null) {
            reconnectJob?.cancel()
            reconnectJob = null
            reconnectAttempts = 0
        }
        val activeSummary = state.activeThreadId?.let { id ->
            snapshot.threads.firstOrNull { it.id == id }
                ?: snapshot.archivedThreads.firstOrNull { it.id == id }
        }
        state = state.copy(
            projects = snapshot.projects,
            threads = snapshot.threads,
            archivedThreads = snapshot.archivedThreads,
            shellLoaded = true,
            connectionStatus = ConnectionStatus.Live,
            connectionNote = null,
            thread = state.thread?.let { current ->
                if (activeSummary != null) current.copy(summary = activeSummary) else current
            },
        )
        cacheShell(snapshot, force = false)
    }

    private fun cacheShell(snapshot: ShellSnapshot, force: Boolean) {
        val config = state.config ?: return
        val base = state.baseUrl
        val environmentId = config.environment.environmentId.ifBlank {
            cachedEnvironmentId.orEmpty()
        }
        if (environmentId.isBlank()) return
        cachedEnvironmentId = environmentId
        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastShellCacheAt < 2_000L) return
        lastShellCacheAt = now
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { clientCache.saveShell(base, environmentId, snapshot) }
        }
    }

    private fun cacheThread(
        snapshot: ThreadDetailSnapshot,
        threadId: String,
        force: Boolean,
        olderItems: List<VisibleTurnItem>? = null,
    ) {
        val config = state.config ?: return
        val environmentId = config.environment.environmentId.ifBlank {
            cachedEnvironmentId.orEmpty()
        }
        if (environmentId.isBlank()) return
        val now = SystemClock.elapsedRealtime()
        val previous = lastThreadCacheAt[threadId] ?: 0L
        if (!force && now - previous < 4_000L) return
        lastThreadCacheAt[threadId] = now
        val base = state.baseUrl
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                clientCache.saveThread(base, environmentId, threadId, snapshot, olderItems)
            }
        }
    }

    fun refresh() {
        if (state.connecting) return
        val conn = connection ?: run {
            if (state.config != null) retryConnection()
            return
        }
        if (shellRefreshJob?.isActive == true) return
        shellRefreshJob = viewModelScope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) { conn.shell() }
                if (connection === conn) applyShell(snapshot)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                if (connection !== conn) return@launch
                val message = "Reconnecting to ${environmentLabel()}…"
                state = state.copy(
                    connectionStatus = ConnectionStatus.Reconnecting,
                    connectionNote = message,
                )
                scheduleReconnect(message)
            }
        }
    }

    /**
     * Refresh provider discovery. Live configs frequently start with empty
     * model lists; the server fills them after refreshProviders.
     */
    fun ensureModelsLoaded(force: Boolean = false, quiet: Boolean = false) {
        val conn = connection ?: return
        if (state.modelsRefreshing) return
        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastProviderRefreshAt < 60_000L) return
        state = state.copy(modelsRefreshing = true)
        modelsJob?.cancel()
        modelsJob = viewModelScope.launch {
            try {
                val config = withContext(Dispatchers.IO) { conn.refreshProviderCatalog() }
                if (connection !== conn) return@launch
                lastProviderRefreshAt = SystemClock.elapsedRealtime()
                state = state.copy(config = config, modelsRefreshing = false)
                val environmentId = config.environment.environmentId.ifBlank {
                    cachedEnvironmentId.orEmpty()
                }
                if (environmentId.isNotBlank()) {
                    cachedEnvironmentId = environmentId
                    val base = state.baseUrl
                    viewModelScope.launch(Dispatchers.IO) {
                        runCatching { clientCache.saveConfig(base, environmentId, config) }
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                if (connection === conn) {
                    state = state.copy(modelsRefreshing = false)
                    if (force && !quiet) toast("Could not load models: ${describe(error)}")
                }
            }
        }
    }

    fun loadPullRequests(force: Boolean = false, stateFilter: String? = null) {
        val conn = connection ?: return
        val current = state.pullRequests
        val filter = stateFilter ?: current.state
        if (current.loading && !force && current.loaded) return
        state = state.copy(
            pullRequests = current.copy(
                loading = true,
                error = null,
                state = filter,
            ),
        )
        pullRequestsJob?.cancel()
        pullRequestsJob = viewModelScope.launch {
            try {
                val payload = buildJsonObject {
                    put("state", filter)
                    put("involvement", "all")
                    put("limit", 100)
                }
                val result = withContext(Dispatchers.IO) {
                    T3Core.json.decodeFromJsonElement<PullRequestListResult>(
                        conn.callObject("pullRequests.list", payload),
                    )
                }
                if (state.tab == AppTab.PullRequests || force) {
                    state = state.copy(
                        pullRequests = PullRequestsUi(
                            entries = result.entries,
                            errors = result.errors,
                            providers = result.providers,
                            loading = false,
                            error = null,
                            state = filter,
                            loaded = true,
                        ),
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                state = state.copy(
                    pullRequests = state.pullRequests.copy(
                        loading = false,
                        error = describe(error),
                        state = filter,
                        loaded = true,
                    ),
                )
            }
        }
    }

    fun setPullRequestsFilter(stateFilter: String) {
        if (stateFilter == state.pullRequests.state && state.pullRequests.loaded) return
        loadPullRequests(force = true, stateFilter = stateFilter)
    }

    // ------------------------------------------------------------ pull request detail

    fun openPullRequest(entry: PullRequestEntry) {
        pullRequestDetailJob?.cancel()
        pullRequestDiffJob?.cancel()
        state = state.copy(
            pullRequestDetail = PullRequestDetailUi(entry = entry),
        )
        loadPullRequestDetail()
        loadPullRequestDiff()
    }

    /**
     * A thread card only carries the linked PR's URL. If the list has not been
     * loaded yet, reconstruct the reference so tapping the card opens the same
     * in-app detail screen instead of dumping the user into a browser.
     */
    fun openPullRequestFromThread(
        url: String,
        projectId: String,
        title: String,
        stateName: String,
        number: Int?,
    ) {
        state.pullRequests.entries.firstOrNull { it.url.equals(url, ignoreCase = true) }
            ?.let { openPullRequest(it) }
            ?: run {
                val parsed = parsePullRequestUrl(url)
                if (parsed == null) {
                    toast("Could not open that pull request.")
                    return
                }
                val project = state.projects.firstOrNull { it.id == projectId }
                val entry = PullRequestEntry(
                    provider = "github",
                    host = parsed.first,
                    projectId = projectId,
                    projectTitle = project?.title.orEmpty(),
                    repository = parsed.second,
                    number = number ?: parsed.third,
                    title = title.ifBlank { "Pull request #${number ?: parsed.third}" },
                    url = url,
                    state = stateName.ifBlank { "open" },
                )
                openPullRequest(entry)
            }
    }

    fun closePullRequest() {
        pullRequestDetailJob?.cancel()
        pullRequestDetailJob = null
        pullRequestDiffJob?.cancel()
        pullRequestDiffJob = null
        state = state.copy(pullRequestDetail = null)
    }

    fun setPullRequestTab(tab: String) {
        val current = state.pullRequestDetail ?: return
        state = state.copy(pullRequestDetail = current.copy(tab = tab))
    }

    fun setPullRequestMergeMethod(method: String) {
        val current = state.pullRequestDetail ?: return
        state = state.copy(pullRequestDetail = current.copy(selectedMethod = method))
    }

    fun loadPullRequestDetail() {
        val current = state.pullRequestDetail ?: return
        val conn = connection ?: return
        val ref = current.entry
        state = state.copy(
            pullRequestDetail = current.copy(loading = current.detail == null, error = null),
        )
        pullRequestDetailJob?.cancel()
        pullRequestDetailJob = viewModelScope.launch {
            try {
                val detail = withContext(Dispatchers.IO) {
                    T3Core.json.decodeFromJsonElement<PullRequestDetail>(
                        conn.callObject("pullRequests.detail", pullRequestRef(ref)),
                    )
                }
                val latest = state.pullRequestDetail ?: return@launch
                if (latest.entry.projectId != ref.projectId ||
                    latest.entry.number != ref.number
                ) return@launch
                val methods = detail.capabilities.mergeMethods
                val selected = when {
                    latest.selectedMethod in methods -> latest.selectedMethod
                    "squash" in methods -> "squash"
                    methods.isNotEmpty() -> methods.first()
                    else -> latest.selectedMethod
                }
                state = state.copy(
                    pullRequestDetail = latest.copy(
                        detail = detail,
                        loading = false,
                        error = null,
                        selectedMethod = selected,
                    ),
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                val latest = state.pullRequestDetail ?: return@launch
                state = state.copy(
                    pullRequestDetail = latest.copy(loading = false, error = describe(error)),
                )
            }
        }
    }

    fun loadPullRequestDiff(cursor: String? = null) {
        val current = state.pullRequestDetail ?: return
        val conn = connection ?: return
        val baseUrl = state.baseUrl
        val token = state.token
        state = state.copy(
            pullRequestDetail = current.copy(
                diffLoading = true,
                diffError = null,
                diff = if (cursor == null) null else current.diff,
            ),
        )
        pullRequestDiffJob?.cancel()
        pullRequestDiffJob = viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    fetchPullRequestDiff(baseUrl, token, current.entry, cursor)
                }
                val latest = state.pullRequestDetail ?: return@launch
                val existing = if (cursor == null) null else latest.diff
                val combined = if (existing == null) result else result.copy(
                    patch = existing.patch.trimEnd() + "\n" + result.patch,
                    omittedFileStats = existing.omittedFileStats + result.omittedFileStats,
                )
                state = state.copy(
                    pullRequestDetail = latest.copy(
                        diff = combined,
                        diffLoading = false,
                        diffError = null,
                    ),
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                val latest = state.pullRequestDetail ?: return@launch
                state = state.copy(
                    pullRequestDetail = latest.copy(
                        diffLoading = false,
                        diffError = describe(error),
                    ),
                )
            }
        }
    }

    fun mergePullRequest(method: String) {
        val current = state.pullRequestDetail ?: return
        if (current.actionBusy) return
        runPullRequestAction("merge", method)
    }

    fun runPullRequestAction(action: String, mergeMethod: String? = null) {
        val current = state.pullRequestDetail ?: return
        val conn = connection ?: return
        if (current.actionBusy) return
        state = state.copy(
            pullRequestDetail = current.copy(actionBusy = true, error = null),
        )
        viewModelScope.launch {
            try {
                val payload = buildJsonObject {
                    put("projectId", current.entry.projectId)
                    if (current.entry.host.isNotBlank()) put("host", current.entry.host)
                    put("repository", current.entry.repository)
                    put("number", current.entry.number)
                    put("action", action)
                    mergeMethod?.let { put("mergeMethod", it) }
                }
                withContext(Dispatchers.IO) { conn.callRaw("pullRequests.runAction", payload) }
                val latest = state.pullRequestDetail ?: return@launch
                state = state.copy(
                    pullRequestDetail = latest.copy(actionBusy = false),
                )
                toast(
                    when (action) {
                        "merge" -> "Pull request merged"
                        "ready" -> "Marked ready for review"
                        "draft" -> "Converted to draft"
                        "close" -> "Pull request closed"
                        "reopen" -> "Pull request reopened"
                        "update-branch" -> "Branch updated"
                        else -> "Pull request updated"
                    },
                )
                loadPullRequestDetail()
                loadPullRequests(force = true, stateFilter = state.pullRequests.state)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                val latest = state.pullRequestDetail
                if (latest != null) {
                    state = state.copy(
                        pullRequestDetail = latest.copy(
                            actionBusy = false,
                            error = describe(error),
                        ),
                    )
                }
                toast(describe(error))
            }
        }
    }

    private fun pullRequestRef(entry: PullRequestEntry): JsonObject = buildJsonObject {
        put("projectId", entry.projectId)
        if (entry.host.isNotBlank()) put("host", entry.host)
        put("repository", entry.repository)
        put("number", entry.number)
    }

    private fun fetchPullRequestDiff(
        baseUrl: String,
        token: String,
        entry: PullRequestEntry,
        cursor: String?,
    ): PullRequestDiffResult {
        val endpoint = "${baseUrl.trimEnd('/')}/api/pull-requests/diff"
        val payload = buildJsonObject {
            put("projectId", entry.projectId)
            if (entry.host.isNotBlank()) put("host", entry.host)
            put("repository", entry.repository)
            put("number", entry.number)
            cursor?.takeIf { it.isNotBlank() }?.let { put("cursor", it) }
        }
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 20_000
            connection.readTimeout = 60_000
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("x-t3-orchestration-protocol", "2")
            connection.outputStream.use { it.write(payload.toString().toByteArray()) }
            val code = connection.responseCode
            val text = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()
            if (code !in 200..299) {
                throw T3Exception(
                    text.takeIf { it.isNotBlank() } ?: "Pull request diff failed ($code).",
                )
            }
            T3Core.json.decodeFromString<PullRequestDiffResult>(text)
        } finally {
            connection.disconnect()
        }
    }

    private fun parsePullRequestUrl(url: String): Triple<String, String, Int>? = runCatching {
        val uri = URI(url)
        val segments = uri.path.trim('/').split('/')
        val number = segments.getOrNull(3)?.toIntOrNull()
        if (uri.host.isNullOrBlank() || segments.size < 4 ||
            segments[2] != "pull" || number == null
        ) {
            null
        } else {
            Triple(uri.host, "${segments[0]}/${segments[1]}", number)
        }
    }.getOrNull()

    // ------------------------------------------------------------------ navigation

    fun selectTab(tab: AppTab) {
        state = state.copy(tab = tab, homeFilterOpen = false)
        when (tab) {
            AppTab.Threads -> refresh()
            AppTab.Usage -> loadUsage(state.usage?.days ?: 30)
            AppTab.PullRequests -> loadPullRequests(force = true)
            else -> Unit
        }
    }

    fun setHomeFilterOpen(open: Boolean) {
        state = state.copy(homeFilterOpen = open)
    }

    fun setProjectFilter(projectId: String?) {
        state = state.copy(projectFilter = projectId)
    }

    fun setSearch(value: String) {
        state = state.copy(search = value)
    }

    fun toggleSettled() {
        state = state.copy(showSettled = !state.showSettled)
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
            var feed: ThreadFeed? = null
            try {
                // Paint the canonical saved transcript before asking the server.
                // Its stored sequence doubles as the resume cursor, so the first
                // subscription can replay only what this device missed.
                var resumeSequence = -1L
                var markedRead = false
                val environmentId = currentEnvironmentId()
                if (environmentId != null) {
                    val cached = withContext(Dispatchers.IO) {
                        clientCache.loadThreadWithHistory(state.baseUrl, environmentId, threadId)
                    }
                    if (cached != null && isActive && state.activeThreadId == threadId) {
                        val current = state.thread
                        if (current != null && current.projection.thread.id.isEmpty()) {
                            applyThreadDetail(threadId, cached.snapshot)
                            if (cached.olderItems.isNotEmpty()) {
                                val latest = state.thread
                                if (latest?.threadId == threadId) {
                                    state = state.copy(
                                        thread = latest.copy(olderItems = cached.olderItems),
                                    )
                                }
                            }
                            if (cached.snapshot.snapshotSequence > 0) {
                                resumeSequence = cached.snapshot.snapshotSequence
                            }
                        }
                    }
                }
                while (isActive && state.activeThreadId == threadId) {
                    val conn = connection
                    if (conn == null) {
                        delay(500)
                        continue
                    }

                    var next: ThreadFeed? = null
                    if (resumeSequence < 0) {
                        // No usable cursor: take the authoritative snapshot.
                        val detail = withContext(Dispatchers.IO) { conn.thread(threadId) }
                        if (!isActive || state.activeThreadId != threadId) return@launch
                        applyThreadDetail(threadId, detail)
                        resumeSequence = detail.snapshotSequence
                        next = try {
                            withContext(Dispatchers.IO) {
                                conn.watchThread(threadId, detail.snapshotSequence, false)
                            }
                        } catch (error: CancellationException) {
                            throw error
                        } catch (error: Throwable) {
                            Log.w(LOG_TAG, "thread subscribe failed for $threadId", error)
                            reportThreadLiveError(threadId, error)
                            delay(1_000)
                            continue
                        }
                    } else {
                        // Warm cache: subscribe after the stored sequence. The
                        // server sends a snapshot only if it cannot replay.
                        next = try {
                            withContext(Dispatchers.IO) {
                                conn.watchThread(threadId, resumeSequence, false)
                            }
                        } catch (error: CancellationException) {
                            throw error
                        } catch (error: Throwable) {
                            Log.w(LOG_TAG, "thread resume failed for $threadId", error)
                            resumeSequence = -1L
                            delay(500)
                            continue
                        }
                    }

                    if (!isActive || state.activeThreadId != threadId) {
                        runCatching { next?.close() }
                        return@launch
                    }
                    val opened = next ?: continue
                    if (!markedRead) {
                        markedRead = true
                        runCatching {
                            withContext(Dispatchers.IO) { conn.threadAction(threadId, "markRead") }
                        }
                    }
                    feed = opened
                    threadFeed = opened
                    setThreadLive(threadId, true)
                    Log.d(LOG_TAG, "thread feed open $threadId after=$resumeSequence")

                    var stateFailures = 0
                    var eventCount = 0
                    var lastStateAt = 0L
                    var lastEventAt = SystemClock.elapsedRealtime()
                    var lastAppliedSequence = resumeSequence
                    // If a subscription is open but silent (proxy, server or
                    // mobile network hiccup), catch up while work is active.
                    val watchdog = launch {
                        while (isActive) {
                            delay(5_000)
                            val current = state.thread?.takeIf { it.threadId == threadId }
                                ?: return@launch
                            val active = current.pendingUserItems.isNotEmpty() ||
                                current.projection.isRunning() ||
                                current.summary?.activeRunId != null
                            if (active &&
                                SystemClock.elapsedRealtime() - lastEventAt >= 4_500L
                            ) {
                                Log.d(LOG_TAG, "thread watchdog refresh $threadId")
                                val catchUp = withContext(Dispatchers.IO) {
                                    runCatching { conn.thread(threadId) }.getOrNull()
                                }
                                if (catchUp != null) applyThreadDetail(threadId, catchUp)
                            }
                        }
                    }
                    try {
                        opened.events.collect { event ->
                            if (!isActive || state.activeThreadId != threadId) return@collect
                            lastEventAt = SystemClock.elapsedRealtime()
                            eventCount++
                            if (eventCount <= 5 || event.kind != "event") {
                                Log.d(
                                    LOG_TAG,
                                    "thread event ${event.kind} seq=${event.sequence} " +
                                        "event#$eventCount id=$threadId",
                                )
                            }
                            if (event.kind == "event" &&
                                event.sequence > 0 &&
                                lastAppliedSequence > event.sequence
                            ) {
                                return@collect
                            }
                            event.snapshot?.let { snapshot ->
                                if (snapshot.projection.thread.id == threadId) {
                                    applyThreadProjection(
                                        threadId,
                                        snapshot.projection,
                                        snapshot.sequence,
                                    )
                                    lastAppliedSequence = maxOf(
                                        lastAppliedSequence,
                                        snapshot.sequence,
                                    )
                                }
                            }
                            val eventType = event.event.domainEventType()
                            var forceReconcile = false
                            event.event?.let { domainEvent ->
                                val current = state.thread
                                    ?.takeIf { it.threadId == threadId }
                                    ?.projection
                                    ?: return@let
                                // A sequence jump means events were dropped. Do
                                // not persist a projection with a hole in it;
                                // reconcile from state before advancing the
                                // resume cursor.
                                val gap = lastAppliedSequence >= 0 &&
                                    event.sequence > 0 &&
                                    event.sequence > lastAppliedSequence + 1
                                forceReconcile = gap
                                applyThreadProjection(
                                    threadId,
                                    current.applyDomainEvent(domainEvent),
                                    event.sequence,
                                    persist = !gap && event.sequence > 0,
                                )
                                if (!gap && event.sequence > 0) {
                                    lastAppliedSequence = maxOf(lastAppliedSequence, event.sequence)
                                }
                            }

                            // Token-sized turn updates are already applied from
                            // their payloads above. Reading the whole projection
                            // for every token is expensive on long threads, so
                            // reconcile with the authoritative state at most
                            // a few times per second. A detected gap always
                            // reconciles immediately.
                            val now = SystemClock.elapsedRealtime()
                            val tokenUpdate = eventType == "turn-item.updated" ||
                                eventType == "message.updated"
                            if (forceReconcile || !tokenUpdate || now - lastStateAt >= 750L) {
                                lastStateAt = now
                                val snapshot = withContext(Dispatchers.IO) {
                                    runCatching { opened.state() }
                                        .onFailure {
                                            Log.w(LOG_TAG, "thread state failed for $threadId", it)
                                        }
                                        .getOrNull()
                                }
                                if (snapshot != null && snapshot.projection.thread.id == threadId) {
                                    stateFailures = 0
                                    applyThreadProjection(
                                        threadId,
                                        snapshot.projection,
                                        snapshot.sequence,
                                    )
                                    lastAppliedSequence = maxOf(
                                        lastAppliedSequence,
                                        snapshot.sequence,
                                    )
                                } else {
                                    stateFailures++
                                    if (stateFailures == 1 || stateFailures % 25 == 0) {
                                        Log.w(
                                            LOG_TAG,
                                            "thread state unusable for $threadId " +
                                                "(seq=${snapshot?.sequence}, " +
                                                "id=${snapshot?.projection?.thread?.id}) " +
                                                "failures=$stateFailures",
                                        )
                                    }
                                    scheduleThreadReconcile(conn, threadId)
                                }
                            }
                        }
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Throwable) {
                        Log.w(LOG_TAG, "thread feed failed for $threadId", error)
                        if (state.activeThreadId == threadId) {
                            state = state.copy(
                                thread = state.thread?.copy(
                                    live = false,
                                    error = "Live updates paused: ${describe(error)}",
                                ),
                            )
                        }
                    } finally {
                        watchdog.cancel()
                        setThreadLive(threadId, false)
                        if (threadFeed === feed) threadFeed = null
                        runCatching { feed?.close() }
                        feed = null
                    }

                    if (!isActive || state.activeThreadId != threadId) return@launch
                    resumeSequence = state.thread
                        ?.takeIf { it.threadId == threadId }
                        ?.sequence
                        ?.takeIf { it > 0 }
                        ?: -1L
                    Log.d(LOG_TAG, "thread feed ended, reconnecting $threadId")
                    delay(1_000)
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
                runCatching { feed?.close() }
                if (threadFeed === feed) threadFeed = null
                threadReconcileJob?.cancel()
                threadReconcileJob = null
            }
        }
    }

    fun refreshThread() {
        val threadId = state.activeThreadId ?: return
        val conn = connection ?: return
        viewModelScope.launch {
            try {
                val detail = withContext(Dispatchers.IO) { conn.thread(threadId) }
                if (state.activeThreadId == threadId) applyThreadDetail(threadId, detail)
            } catch (error: Throwable) {
                toast(describe(error))
            }
        }
    }

    private fun applyThreadDetail(threadId: String, detail: ThreadDetailSnapshot) {
        val current = state.thread ?: return
        if (current.threadId != threadId || state.activeThreadId != threadId) return
        applyThreadProjection(threadId, detail.projection, detail.snapshotSequence) { latest ->
            latest.copy(
                hasMoreHistory = latest.hasMoreHistory || detail.hasMoreHistory == true,
                historyCursor = detail.historyCursor ?: latest.historyCursor,
            )
        }
    }

    private fun applyThreadProjection(
        threadId: String,
        projection: ThreadProjection,
        sequence: Long,
        persist: Boolean = true,
        extra: (ThreadUi) -> ThreadUi = { it },
    ) {
        val current = state.thread ?: return
        if (current.threadId != threadId || state.activeThreadId != threadId) return
        val hasContent = projection.thread.id.isNotEmpty() ||
            projection.messages.isNotEmpty() ||
            projection.visibleTurnItems.isNotEmpty()
        if (!hasContent) return
        val pending = current.pendingUserItems.filterNot { pendingItem ->
            val text = pendingItem.item.text.trim()
            val inTimeline = projection.visibleTurnItems.any { row ->
                row.item.type == "user_message" &&
                    (text.isEmpty() || row.item.text.trim() == text)
            }
            val inMessages = projection.messages.any { message ->
                message.role == "user" && (text.isEmpty() || message.text.trim() == text)
            }
            if (projection.visibleTurnItems.isNotEmpty()) inTimeline else inMessages
        }
        val latest = current.copy(
            projection = projection,
            summary = state.summaryFor(threadId) ?: current.summary,
            loading = false,
            error = null,
            sequence = maxOf(current.sequence, sequence),
            committedSequence = if (persist) maxOf(current.committedSequence, sequence)
            else current.committedSequence,
            pendingUserItems = pending,
        )
        val applied = extra(latest)
        state = state.copy(thread = applied)
        if (persist && projection.thread.id.isNotEmpty() && sequence >= current.committedSequence) {
            cacheThread(
                ThreadDetailSnapshot(
                    snapshotSequence = sequence,
                    projection = projection,
                    historyCursor = applied.historyCursor,
                    hasMoreHistory = applied.hasMoreHistory,
                ),
                threadId,
                force = false,
            )
        }
    }

    private fun currentEnvironmentId(): String? {
        val configured = state.config?.environment?.environmentId?.takeIf { it.isNotBlank() }
        return configured ?: cachedEnvironmentId?.takeIf { it.isNotBlank() }
    }

    private fun setThreadLive(threadId: String, live: Boolean) {
        val current = state.thread ?: return
        if (current.threadId == threadId && current.live != live) {
            state = state.copy(thread = current.copy(live = live))
        }
    }

    private fun reportThreadLiveError(threadId: String, error: Throwable) {
        if (state.activeThreadId != threadId) return
        state = state.copy(
            thread = state.thread?.copy(
                loading = false,
                live = false,
                error = "Live updates unavailable: ${describe(error)}",
            ),
        )
    }

    private fun scheduleThreadReconcile(conn: T3Connection, threadId: String) {
        if (threadReconcileJob?.isActive == true) return
        threadReconcileJob = viewModelScope.launch {
            delay(350)
            val detail = withContext(Dispatchers.IO) {
                runCatching { conn.thread(threadId) }.getOrNull()
            } ?: return@launch
            if (state.activeThreadId == threadId) applyThreadDetail(threadId, detail)
        }
    }

    fun closeThread(persist: Boolean = true) {
        val current = state.thread
        val environmentId = currentEnvironmentId()
        if (persist &&
            current != null &&
            current.projection.thread.id.isNotEmpty() &&
            current.committedSequence > 0 &&
            current.sequence <= current.committedSequence &&
            environmentId != null
        ) {
            val snapshot = ThreadDetailSnapshot(
                snapshotSequence = current.committedSequence,
                projection = current.projection,
                historyCursor = current.historyCursor,
                hasMoreHistory = current.hasMoreHistory,
            )
            val base = state.baseUrl
            val olderItems = current.olderItems
            viewModelScope.launch(Dispatchers.IO) {
                runCatching {
                    clientCache.saveThread(
                        base,
                        environmentId,
                        current.threadId,
                        snapshot,
                        olderItems,
                    )
                }
            }
        }
        threadJob?.cancel()
        threadJob = null
        threadFeed?.close()
        threadFeed = null
        threadReconcileJob?.cancel()
        threadReconcileJob = null
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
                val updated = latest.copy(
                    olderItems = merged,
                    hasMoreHistory = page.hasMoreHistory,
                    historyCursor = page.nextCursor,
                    loadingEarlier = false,
                )
                state = state.copy(thread = updated)
                val environmentId = currentEnvironmentId()
                if (environmentId != null) {
                    val snapshot = ThreadDetailSnapshot(
                        snapshotSequence = updated.committedSequence,
                        projection = updated.projection,
                        historyCursor = updated.historyCursor,
                        hasMoreHistory = updated.hasMoreHistory,
                    )
                    val base = state.baseUrl
                    viewModelScope.launch(Dispatchers.IO) {
                        runCatching {
                            clientCache.saveThread(
                                base,
                                environmentId,
                                updated.threadId,
                                snapshot,
                                merged,
                            )
                        }
                    }
                }
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
            current.projection.thread.status == "running" ||
            current.projection.isRunning()
        val pendingId = "local:${UUID.randomUUID()}"
        val ordinal = (current.projection.turnItems.maxOfOrNull { it.ordinal } ?: 0) +
            current.pendingUserItems.size + 1
        val pending = VisibleTurnItem(
            position = current.projection.visibleTurnItems.size + current.pendingUserItems.size,
            visibility = "local",
            sourceItemId = pendingId,
            sourceThreadId = current.threadId,
            item = TurnItem(
                id = pendingId,
                threadId = current.threadId,
                ordinal = ordinal,
                status = "pending",
                type = "user_message",
                text = trimmed,
                attachments = current.pendingAttachments.map { T3Core.json.encodeToJsonElement(it) },
            ),
        )
        state = state.copy(
            thread = current.copy(
                sending = true,
                error = null,
                pendingUserItems = current.pendingUserItems + pending,
            ),
        )
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
                val latest = state.thread
                if (latest?.threadId == current.threadId) {
                    state = state.copy(
                        thread = latest.copy(
                            sending = false,
                            pendingAttachments = emptyList(),
                        ),
                    )
                }
            } catch (error: Throwable) {
                val latest = state.thread
                if (latest?.threadId == current.threadId) {
                    state = state.copy(
                        thread = latest.copy(
                            sending = false,
                            error = describe(error),
                            pendingUserItems = latest.pendingUserItems.filterNot {
                                it.sourceItemId == pendingId
                            },
                        ),
                    )
                }
            }
        }
    }

    fun stopRun() {
        val current = state.thread ?: return
        val conn = connection ?: return
        val runId = current.projection.activeRunId(current.summary ?: state.summaryFor(current.threadId))
        if (runId.isNullOrBlank()) {
            toast("No active run to stop.")
            return
        }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { conn.interruptRun(current.threadId, runId) }
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

    fun respondUserInput(requestId: String, answers: JsonObject) {
        val current = state.thread ?: return
        if (current.inputBusyRequestId != null) return
        val request = current.projection.pendingRequests.firstOrNull { it.id == requestId } ?: return
        if (request.kind != "user_input") return
        val conn = connection ?: return
        state = state.copy(thread = current.copy(inputBusyRequestId = requestId))
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    conn.respondToRequest(
                        threadId = current.threadId,
                        requestId = requestId,
                        decision = "",
                        answers = answers,
                    )
                }
                toast("Answer sent")
            } catch (error: Throwable) {
                toast(describe(error))
            } finally {
                val latest = state.thread
                if (latest?.threadId == current.threadId) {
                    state = state.copy(thread = latest.copy(inputBusyRequestId = null))
                }
            }
        }
    }

    fun dismissUserInput(requestId: String) {
        val current = state.thread ?: return
        if (current.inputBusyRequestId != null) return
        val conn = connection ?: return
        state = state.copy(thread = current.copy(inputBusyRequestId = requestId))
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    conn.dismissUserInput(current.threadId, requestId)
                }
                toast("Question dismissed")
            } catch (error: Throwable) {
                toast(describe(error))
            } finally {
                val latest = state.thread
                if (latest?.threadId == current.threadId) {
                    state = state.copy(thread = latest.copy(inputBusyRequestId = null))
                }
            }
        }
    }

    fun threadAction(threadId: String, action: String, value: String = "") {
        val conn = connection ?: return
        applyLocalThreadAction(threadId, action, value)
        when (action) {
            "archive" -> if (state.activeThreadId == threadId) closeThread()
            "delete" -> if (state.activeThreadId == threadId) closeThread(persist = false)
        }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { conn.threadAction(threadId, action, value) }
                when (action) {
                    "archive" -> toast("Thread archived")
                    "unarchive" -> toast("Thread restored")
                    "delete" -> {
                        val environmentId = currentEnvironmentId()
                        if (environmentId != null) {
                            withContext(Dispatchers.IO) {
                                clientCache.removeThread(state.baseUrl, environmentId, threadId)
                            }
                        }
                        toast("Thread deleted")
                    }
                    "rename" -> toast("Thread renamed")
                    "pin" -> toast("Thread pinned")
                    "unpin" -> toast("Thread unpinned")
                    "settle" -> toast("Thread settled")
                    "unsettle" -> toast("Thread moved back to chats")
                    "snooze" -> toast("Thread snoozed")
                    "unsnooze" -> toast("Thread unsnoozed")
                    "setModel" -> toast("Model changed")
                    "markRead", "markUnread", "setRuntimeMode", "setInteractionMode" -> Unit
                }
                refresh()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                toast(describe(error))
                if (state.activeThreadId == threadId) refreshThread()
                refresh()
            }
        }
    }

    /**
     * Keep list and open-thread state in sync before the server round trip.
     * The live snapshot reconciles immediately after the command succeeds.
     */
    private fun applyLocalThreadAction(threadId: String, action: String, value: String) {
        val existing = state.summaryFor(threadId)
            ?: state.thread?.summary?.takeIf { it.id == threadId }
            ?: return
        val now = Instant.now().toString()
        var threads = state.threads
        var archived = state.archivedThreads
        var updated: ThreadShell? = existing

        fun List<ThreadShell>.updatedThread(transform: (ThreadShell) -> ThreadShell) =
            map { thread -> if (thread.id == threadId) transform(thread) else thread }

        when (action) {
            "archive" -> {
                val next = existing.copy(archivedAt = now, updatedAt = now)
                threads = threads.filterNot { it.id == threadId }
                archived = archived.filterNot { it.id == threadId } + next
                updated = next
            }
            "unarchive" -> {
                val next = existing.copy(archivedAt = null, updatedAt = now)
                archived = archived.filterNot { it.id == threadId }
                threads = threads.filterNot { it.id == threadId } + next
                updated = next
            }
            "delete" -> {
                threads = threads.filterNot { it.id == threadId }
                archived = archived.filterNot { it.id == threadId }
                updated = null
            }
            "settle" -> {
                val next = existing.copy(
                    settledAt = now,
                    settledOverride = "settled",
                    unsettledAt = null,
                    updatedAt = now,
                )
                threads = threads.updatedThread { next }
                archived = archived.updatedThread { next }
                updated = next
            }
            "unsettle" -> {
                val next = existing.copy(
                    settledAt = null,
                    settledOverride = null,
                    unsettledAt = now,
                    updatedAt = now,
                )
                threads = threads.updatedThread { next }
                archived = archived.updatedThread { next }
                updated = next
            }
            "snooze" -> {
                val next = existing.copy(snoozedAt = now, snoozedUntil = value, updatedAt = now)
                threads = threads.updatedThread { next }
                archived = archived.updatedThread { next }
                updated = next
            }
            "unsnooze" -> {
                val next = existing.copy(snoozedAt = null, snoozedUntil = null, updatedAt = now)
                threads = threads.updatedThread { next }
                archived = archived.updatedThread { next }
                updated = next
            }
            "rename" -> {
                val next = existing.copy(title = value, updatedAt = now)
                threads = threads.updatedThread { next }
                archived = archived.updatedThread { next }
                updated = next
            }
            "pin" -> {
                val next = existing.copy(pinnedAt = now, pinOrderKey = now, updatedAt = now)
                threads = threads.updatedThread { next }
                archived = archived.updatedThread { next }
                updated = next
            }
            "unpin" -> {
                val next = existing.copy(pinnedAt = null, pinOrderKey = null, updatedAt = now)
                threads = threads.updatedThread { next }
                archived = archived.updatedThread { next }
                updated = next
            }
            "markRead" -> {
                val next = existing.copy(lastVisitedAt = now)
                threads = threads.updatedThread { next }
                archived = archived.updatedThread { next }
                updated = next
            }
            "markUnread" -> {
                val next = existing.copy(lastVisitedAt = null)
                threads = threads.updatedThread { next }
                archived = archived.updatedThread { next }
                updated = next
            }
            "setRuntimeMode" -> {
                val next = existing.copy(runtimeMode = value, updatedAt = now)
                threads = threads.updatedThread { next }
                archived = archived.updatedThread { next }
                updated = next
            }
            "setInteractionMode" -> {
                val next = existing.copy(interactionMode = value, updatedAt = now)
                threads = threads.updatedThread { next }
                archived = archived.updatedThread { next }
                updated = next
            }
            "setModel" -> {
                val selection = runCatching {
                    T3Core.json.decodeFromString<ModelSelection>(value)
                }.getOrNull()
                if (selection != null) {
                    val next = existing.copy(modelSelection = selection, updatedAt = now)
                    threads = threads.updatedThread { next }
                    archived = archived.updatedThread { next }
                    updated = next
                }
            }
        }

        val active = state.thread
        val nextThread = active?.let { current ->
            if (current.threadId != threadId || updated == null) {
                current
            } else {
                current.copy(
                    summary = updated,
                    projection = current.projection.copy(thread = updated),
                )
            }
        }
        state = state.copy(
            threads = threads,
            archivedThreads = archived,
            thread = nextThread,
        )
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
                    if ((fileSize(resolver, uri) ?: 0L) > MAX_ATTACHMENT_BYTES) {
                        throw T3Exception("Attachments are limited to ${MAX_ATTACHMENT_BYTES / (1024 * 1024)} MB.")
                    }
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
                if (latest.threadId != current.threadId) return@launch
                state = state.copy(
                    thread = latest.copy(
                        uploading = false,
                        pendingAttachments = latest.pendingAttachments + attachment,
                    ),
                )
            } catch (error: Throwable) {
                val latest = state.thread ?: return@launch
                if (latest.threadId != current.threadId) return@launch
                state = state.copy(
                    thread = latest.copy(uploading = false, error = describe(error)),
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
        branchesJob?.cancel()
        branchesJob = null
        state = state.copy(newThreadOpen = false, newThreadBranchesLoading = false)
    }

    fun setNewThreadProject(projectId: String) {
        if (state.newThreadProjectId != projectId) {
            branchesJob?.cancel()
            state = state.copy(
                newThreadProjectId = projectId,
                newThreadBranches = emptyList(),
                newThreadBranch = "",
                newThreadBranchesLoading = false,
                newThreadBranchesError = null,
            )
        }
        loadNewThreadBranches(projectId)
    }

    fun setNewThreadBranch(branch: String) {
        if (state.newThreadBranch != branch) state = state.copy(newThreadBranch = branch)
    }

    /**
     * Loads local and remote branches for the project checkout. Worktree
     * launches must send a non-empty baseRef, so the New thread screen can only
     * enable "New worktree" once a base branch is known.
     */
    fun loadNewThreadBranches(projectId: String = state.newThreadProjectId.orEmpty()) {
        val project = state.projects.firstOrNull { it.id == projectId } ?: return
        val cwd = project.workspaceRoot.trim()
        if (cwd.isEmpty()) {
            state = state.copy(
                newThreadBranches = emptyList(),
                newThreadBranch = "",
                newThreadBranchesLoading = false,
                newThreadBranchesError = "This project has no workspace path.",
            )
            return
        }
        val conn = connection ?: return
        if (state.newThreadBranchesLoading) return
        branchesJob?.cancel()
        state = state.copy(
            newThreadBranchesLoading = true,
            newThreadBranchesError = null,
        )
        branchesJob = viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    conn.listRefs(cwd = cwd, limit = 200)
                }
                if (state.newThreadProjectId != projectId) return@launch
                val refs = result.refs.filter { it.name.isNotBlank() }
                val preferred = refs.firstOrNull { it.isDefault }?.name
                    ?: refs.firstOrNull { it.current }?.name
                    ?: refs.firstOrNull { !it.isRemote }?.name
                    ?: refs.firstOrNull()?.name
                    ?: ""
                val keep = state.newThreadBranch.takeIf { branch ->
                    refs.any { it.name == branch }
                }
                state = state.copy(
                    newThreadBranches = refs,
                    newThreadBranch = keep ?: preferred,
                    newThreadBranchesLoading = false,
                    newThreadBranchesError = when {
                        !result.isRepo -> "This project is not a git repository."
                        refs.isEmpty() -> "No branches were found in this repository."
                        else -> null
                    },
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                if (state.newThreadProjectId != projectId) return@launch
                state = state.copy(
                    newThreadBranches = emptyList(),
                    newThreadBranch = "",
                    newThreadBranchesLoading = false,
                    newThreadBranchesError = "Could not load branches. ${describe(error)}",
                )
            }
        }
    }

    fun launchThread(
        projectId: String,
        title: String,
        selection: ModelSelection,
        initialText: String,
        workspaceType: String = "root",
        baseRef: String = "",
    ) {
        if (state.launching) return
        val conn = connection ?: return
        val workspace = if (workspaceType == "worktree") {
            if (baseRef.isBlank()) {
                toast("Choose a base branch before creating a worktree.")
                return
            }
            WorkspaceStrategy(type = "worktree", baseRef = baseRef)
        } else {
            WorkspaceStrategy(type = workspaceType)
        }
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
                        workspace = workspace,
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
        val cwd = resolveCwd(threadId)
        if (cwd != null) {
            startTerminal(conn, threadId, cwd)
            return
        }
        viewModelScope.launch {
            val resolved = resolveWorkspacePath(conn, threadId)
            if (resolved == null) {
                toast("This thread has no workspace path yet. Try again after it finishes loading.")
                return@launch
            }
            startTerminal(conn, threadId, resolved)
        }
    }

    private fun startTerminal(conn: T3Connection, threadId: String, cwd: String) {
        closeTerminal()
        state = state.copy(terminal = TerminalUi(threadId = threadId, cwd = cwd))
        terminalJob = viewModelScope.launch {
            var feed: TerminalFeed? = null
            try {
                val next = withContext(Dispatchers.IO) {
                    runCatching { conn.openTerminal(threadId = threadId, cwd = cwd, cols = 80, rows = 24) }
                    conn.attachTerminal(threadId = threadId, cwd = cwd, cols = 80, rows = 24)
                }
                if (!isActive || state.terminal?.threadId != threadId) {
                    runCatching { next.close() }
                    return@launch
                }
                feed = next
                terminalFeed = next
                state = state.copy(terminal = state.terminal?.copy(connected = true))
                next.events.collect { event ->
                    val terminal = state.terminal ?: return@collect
                    if (terminal.threadId != threadId) return@collect
                    when (event.type) {
                        "output" -> appendTerminal(terminal, stripAnsi(event.data))
                        "snapshot" -> {
                            val history = event.snapshot?.history
                            val latest = state.terminal
                            if (history != null && latest?.threadId == threadId) {
                                state = state.copy(
                                    terminal = latest.copy(
                                        text = stripAnsi(history).takeLast(MAX_TERMINAL_CHARS),
                                    ),
                                )
                            }
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
                val latest = state.terminal
                if (latest?.threadId == threadId) {
                    state = state.copy(terminal = latest.copy(error = describe(error)))
                }
            } finally {
                runCatching { feed?.close() }
                if (terminalFeed === feed) terminalFeed = null
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
        val cwd = resolveCwd(threadId)
        if (cwd != null) {
            startDiff(conn, threadId, cwd)
            return
        }
        viewModelScope.launch {
            val resolved = resolveWorkspacePath(conn, threadId)
            if (resolved == null) {
                toast("This thread has no workspace path yet. Try again after it finishes loading.")
                return@launch
            }
            startDiff(conn, threadId, resolved)
        }
    }

    private fun startDiff(conn: T3Connection, threadId: String, cwd: String) {
        diffJob?.cancel()
        state = state.copy(diff = DiffUi(threadId = threadId, cwd = cwd, loading = true))
        diffJob = viewModelScope.launch {
            try {
                val preview = withContext(Dispatchers.IO) { conn.diffPreview(cwd) }
                val latest = state.diff
                if (latest?.threadId == threadId) {
                    state = state.copy(
                        diff = latest.copy(preview = preview, loading = false, error = null),
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                val latest = state.diff
                if (latest?.threadId == threadId) {
                    state = state.copy(
                        diff = latest.copy(loading = false, error = describe(error)),
                    )
                }
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
        val conn = connection ?: return
        val cwd = resolveCwd(threadId)
        if (cwd != null) {
            startFiles(threadId, cwd)
            return
        }
        viewModelScope.launch {
            val resolved = resolveWorkspacePath(conn, threadId)
            if (resolved == null) {
                toast("This thread has no workspace path yet. Try again after it finishes loading.")
                return@launch
            }
            startFiles(threadId, resolved)
        }
    }

    private fun startFiles(threadId: String, cwd: String) {
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
                        entries = immediateEntries(result.entries, directory),
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

    /**
     * The server answers a directory listing with every descendant. The
     * official client only renders entries whose parent is the requested
     * directory and loads children when a folder is expanded; without this
     * the first Files page was an unstructured dump of the whole repository.
     */
    private fun immediateEntries(entries: List<ProjectEntry>, directory: String): List<ProjectEntry> {
        val parent = directory.trim('/').trim()
        return entries.filter { entry ->
            val path = entry.path.trim('/').trim()
            path.isNotEmpty() && path.substringBeforeLast('/', missingDelimiterValue = "") == parent
        }
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
        val active = state.thread?.takeIf { it.threadId == threadId }
        val detail = active?.projection?.thread
        val summary = active?.summary?.takeIf { it.id == threadId }
            ?: detail?.takeIf { it.id.isNotEmpty() }
            ?: state.summaryFor(threadId)
        val projectId = summary?.projectId ?: detail?.projectId
        val project = projectId?.let { id -> state.projects.firstOrNull { it.id == id } }
            ?: state.projectFor(summary)
        return detail?.worktreePath?.takeIf { it.isNotBlank() }
            ?: summary?.worktreePath?.takeIf { it.isNotBlank() }
            ?: project?.workspaceRoot?.takeIf { it.isNotBlank() }
            ?: state.config?.scratchWorkspaceRoot?.takeIf { it.isNotBlank() }
            ?: state.config?.cwd?.takeIf { it.isNotBlank() }
    }

    /**
     * A cached shell can lag behind a thread that was just created (or a
     * project that was just added). Resolve from the live thread/project
     * endpoints before giving up, which is what made Files, Terminal and
     * review surface "no workspace path" straight after opening a thread.
     */
    private suspend fun resolveWorkspacePath(conn: T3Connection, threadId: String): String? {
        resolveCwd(threadId)?.let { return it }
        val detail = withContext(Dispatchers.IO) {
            runCatching { conn.thread(threadId) }.getOrNull()
        }
        if (detail != null && state.activeThreadId == threadId) {
            applyThreadDetail(threadId, detail)
        }
        val projectId = detail?.projection?.thread?.projectId
            ?: state.summaryFor(threadId)?.projectId
        var projects = state.projects
        if (projectId != null && projects.none { it.id == projectId }) {
            val loaded = withContext(Dispatchers.IO) {
                runCatching { conn.projects().projects }.getOrNull()
            }
            if (loaded != null) {
                projects = loaded
                state = state.copy(projects = loaded)
            }
        }
        val project = projectId?.let { id -> projects.firstOrNull { it.id == id } }
        return detail?.projection?.thread?.worktreePath?.takeIf { it.isNotBlank() }
            ?: state.summaryFor(threadId)?.worktreePath?.takeIf { it.isNotBlank() }
            ?: project?.workspaceRoot?.takeIf { it.isNotBlank() }
            ?: state.config?.scratchWorkspaceRoot?.takeIf { it.isNotBlank() }
            ?: state.config?.cwd?.takeIf { it.isNotBlank() }
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
        threadReconcileJob?.cancel()
        threadReconcileJob = null
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
        pullRequestsJob?.cancel()
        pullRequestsJob = null
        shellRefreshJob?.cancel()
        shellRefreshJob = null
        modelsJob?.cancel()
        modelsJob = null
        connection?.close()
        connection = null
    }

    override fun onCleared() {
        stopAllStreams()
        clientCache.close()
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

    private fun fileSize(resolver: ContentResolver, uri: Uri): Long? = runCatching {
        resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (index >= 0 && !cursor.isNull(index)) cursor.getLong(index) else null
            } else {
                null
            }
        }
    }.getOrNull()

    private fun describe(error: Throwable): String {
        val raw = when (error) {
            is T3Exception ->
                if (error.tag.isNullOrEmpty()) error.message ?: "request failed"
                else "${error.message} (${error.tag})"
            is UnsatisfiedLinkError -> "This build only ships the arm64-v8a core (${error.message})."
            else -> error.message ?: error.javaClass.simpleName
        }
        if (!raw.startsWith("server defect", ignoreCase = true)) {
            return raw.lineSequence().firstOrNull()?.trim().orEmpty().let {
                if (it.length > 280) it.take(280).trimEnd() + "…" else it
            }.ifBlank { "request failed" }
        }
        Log.w(LOG_TAG, "server rejected an action: $raw")
        return if (raw.contains("Expected")) {
            "The server rejected this action. Update the app or the T3 server and try again."
        } else {
            raw.lineSequence().firstOrNull()?.trim().orEmpty().ifBlank { "The server rejected this action." }
        }
    }

    private companion object {
        const val LOG_TAG = "T3Live"
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
