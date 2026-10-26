package com.t3codes.mobile

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.t3codes.core.ShellFeed
import com.t3codes.core.ShellSnapshot
import com.t3codes.core.T3Connection
import com.t3codes.core.T3Core
import com.t3codes.core.ThreadDetailSnapshot
import com.t3codes.core.ThreadProjection
import com.t3codes.core.ThreadShell
import com.t3codes.core.ThreadState
import com.t3codes.core.ThreadFeed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Optional foreground sync. It owns a second, independent native connection so
 * the live UI can be destroyed or backgrounded without losing shell updates,
 * active thread feeds, cache writes, or completion notifications.
 */
class BackgroundSyncService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val watchers = ConcurrentHashMap<String, Job>()
    private var syncJob: Job? = null
    private var connection: T3Connection? = null
    private var shellFeed: ShellFeed? = null
    private var cache: ClientCache? = null
    private var connectedBase: String = ""
    private var latestSnapshot: ShellSnapshot? = null
    private var previousActive = mutableMapOf<String, String>()
    private val knownRequests = mutableSetOf<String>()
    private val notifiedRuns = mutableSetOf<String>()
    private var lastShellSavedAt = 0L
    private var foregroundStarted = false

    override fun onCreate() {
        super.onCreate()
        T3Notifications.ensureChannels(this)
        cache = ClientCache(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        T3Notifications.ensureChannels(this)
        try {
            startForegroundCompat()
        } catch (error: Throwable) {
            Log.w(LOG_TAG, "could not start foreground sync", error)
            stopSelf()
            return START_NOT_STICKY
        }
        startSync()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        syncJob?.cancel()
        syncJob = null
        watchers.values.forEach { it.cancel() }
        watchers.clear()
        runCatching { shellFeed?.close() }
        shellFeed = null
        runCatching { connection?.close() }
        connection = null
        scope.cancel()
        runCatching { cache?.close() }
        cache = null
        if (foregroundStarted) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            foregroundStarted = false
        }
        super.onDestroy()
    }

    private fun startSync() {
        if (syncJob?.isActive == true) return
        syncJob = scope.launch {
            var backoff = 1_000L
            while (isActive) {
                val prefs = AppPreferences.of(this@BackgroundSyncService)
                val enabled = prefs.getBoolean(AppPreferences.BACKGROUND_SYNC, false)
                val base = prefs.getString(AppPreferences.BASE_URL, "").orEmpty().trim()
                val token = prefs.getString(AppPreferences.TOKEN, "").orEmpty().trim()
                if (!enabled || base.isBlank() || token.isBlank()) {
                    stopSelf()
                    break
                }

                var next: T3Connection? = null
                var feed: ShellFeed? = null
                try {
                    next = T3Core.connect(base, token, "T3 Code Android background")
                    if (!isActive) {
                        next.close()
                        break
                    }
                    val initialSnapshot = next.shell()
                    if (!isActive) break
                    connectedBase = base
                    connection = next
                    processSnapshot(initialSnapshot, initial = latestSnapshot == null)
                    backoff = 1_000L
                    updatePersistent()

                    feed = next.watchShell()
                    shellFeed = feed
                    try {
                        feed.events.collect {
                            processSnapshot(feed.snapshot(), initial = false)
                        }
                    } finally {
                        runCatching { feed.close() }
                        if (shellFeed === feed) shellFeed = null
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Throwable) {
                    Log.w(LOG_TAG, "background sync disconnected", error)
                    if (isActive) updatePersistent(reconnecting = true)
                } finally {
                    if (connection === next) connection = null
                    runCatching { next?.close() }
                }

                if (!isActive) break
                delay(backoff)
                backoff = (backoff * 2).coerceAtMost(30_000L)
            }
        }
    }

    private fun processSnapshot(snapshot: ShellSnapshot, initial: Boolean) {
        latestSnapshot = snapshot
        saveShell(snapshot, force = initial)

        val allThreads = snapshot.threads + snapshot.archivedThreads
        val active = buildMap {
            allThreads.forEach { thread ->
                thread.activeRunId?.takeIf { it.isNotBlank() }?.let { runId ->
                    put(thread.id, runId)
                }
            }
        }

        if (!initial) {
            previousActive.forEach { (threadId, runId) ->
                if (active[threadId] != runId) {
                    allThreads.firstOrNull { it.id == threadId }?.let { thread ->
                        notifyCompletion(thread, runId)
                    }
                }
            }
        }
        previousActive = active.toMutableMap()

        processPendingRequests(allThreads)
        syncWatchers(active.keys)
        updatePersistent(activeCount = active.size)
    }

    private fun processPendingRequests(threads: List<ThreadShell>) {
        val seen = mutableSetOf<String>()
        threads.forEach { thread ->
            val request = thread.pendingRuntimeRequest ?: return@forEach
            if (request.status == "resolved" || request.id.isBlank()) return@forEach
            seen += request.id
            if (knownRequests.add(request.id)) {
                notifyInput(thread, request.kind)
            }
        }
        val stale = knownRequests - seen
        stale.forEach { requestId ->
            knownRequests.remove(requestId)
            NotificationManagerCompat.from(this).cancel(inputNotificationId(requestId))
        }
    }

    private fun syncWatchers(activeThreadIds: Set<String>) {
        activeThreadIds.forEach(::startWatcher)
        watchers.keys.filter { it !in activeThreadIds }.forEach { threadId ->
            watchers.remove(threadId)?.cancel()
        }
    }

    private fun startWatcher(threadId: String) {
        if (watchers[threadId]?.isActive == true) return
        watchers[threadId] = scope.launch {
            var feed: ThreadFeed? = null
            try {
                val conn = connection ?: return@launch
                val detail = conn.thread(threadId)
                saveThread(threadId, detail, force = true)
                val activeFeed = conn.watchThread(
                    threadId = threadId,
                    afterSequence = detail.snapshotSequence,
                    acceptBounded = true,
                )
                feed = activeFeed
                var lastSequence = detail.snapshotSequence
                var lastSavedAt = 0L
                activeFeed.events.collect { event ->
                    if (event.kind == "closed") return@collect
                    val state = event.snapshot ?: activeFeed.state()
                    if (state.sequence < lastSequence) return@collect
                    lastSequence = state.sequence
                    val now = SystemClock.elapsedRealtime()
                    if (now - lastSavedAt >= 3_000L) {
                        lastSavedAt = now
                        saveThreadState(threadId, state)
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                Log.w(LOG_TAG, "background thread watch stopped: $threadId", error)
            } finally {
                runCatching { feed?.close() }
            }
        }
    }

    private fun saveShell(snapshot: ShellSnapshot, force: Boolean) {
        val conn = connection ?: return
        val environmentId = conn.config.environment.environmentId
        if (environmentId.isBlank() || connectedBase.isBlank()) return
        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastShellSavedAt < 3_000L) return
        lastShellSavedAt = now
        val base = connectedBase
        scope.launch {
            runCatching { cache?.saveShell(base, environmentId, snapshot) }
        }
    }

    private fun saveThread(threadId: String, detail: ThreadDetailSnapshot, force: Boolean) {
        val conn = connection ?: return
        val environmentId = conn.config.environment.environmentId
        if (environmentId.isBlank() || connectedBase.isBlank()) return
        val base = connectedBase
        scope.launch {
            runCatching { cache?.saveThread(base, environmentId, threadId, detail) }
        }
    }

    private fun saveThreadState(threadId: String, state: ThreadState) {
        saveThread(
            threadId = threadId,
            detail = ThreadDetailSnapshot(
                snapshotSequence = state.sequence,
                projection = state.projection,
                historyCursor = state.historyCursor,
                hasMoreHistory = state.hasMoreHistory,
            ),
            force = true,
        )
    }

    private fun notifyCompletion(thread: ThreadShell, runId: String) {
        if (!notifiedRuns.add(runId)) return
        if (notifiedRuns.size > 500) {
            notifiedRuns.clear()
            notifiedRuns.add(runId)
        }
        watchers.remove(thread.id)?.cancel()
        scope.launch {
            val conn = connection
            val detail = if (conn != null) {
                runCatching { conn.thread(thread.id) }.getOrNull()
            } else {
                null
            }
            detail?.let { saveThread(thread.id, it, force = true) }
            if (!shouldAlert()) return@launch

            val fallback = thread.latestVisibleMessage
                ?.takeIf { it.role == "assistant" && it.text.isNotBlank() }
                ?.text
            val error = thread.lastError?.takeIf { it.isNotBlank() }
            val body = when {
                error != null -> error
                detail != null -> detail.projection.notificationBody()
                !fallback.isNullOrBlank() -> fallback.concludingParagraph()
                else -> "The run finished."
            }
            val title = thread.title.ifBlank { "T3 Code run finished" }
            val notification = NotificationCompat.Builder(this@BackgroundSyncService, T3Notifications.CHANNEL_COMPLETION)
                .setSmallIcon(R.drawable.ic_stat_t3)
                .setContentTitle(title)
                .setContentText(if (error != null) "Run failed · $body" else body)
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(
                        if (error != null) "Run failed\n\n$body" else body,
                    ),
                )
                .setSubText(if (error != null) "Run failed" else "Run finished")
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(T3Notifications.contentIntent(this@BackgroundSyncService, thread.id))
                .build()
            NotificationManagerCompat.from(this@BackgroundSyncService)
                .notify(completionNotificationId(runId), notification)
        }
    }

    private fun notifyInput(thread: ThreadShell, kind: String) {
        if (!shouldAlert()) return
        val request = thread.pendingRuntimeRequest ?: return
        val isQuestion = kind == "user_input"
        val label = if (isQuestion) "Input needed" else "Approval needed"
        val body = if (isQuestion) {
            "The agent is waiting for your answer."
        } else {
            "The agent is waiting for your decision before it continues."
        }
        val title = thread.title.ifBlank { label }
        val notification = NotificationCompat.Builder(this, T3Notifications.CHANNEL_INPUT)
            .setSmallIcon(R.drawable.ic_stat_t3)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setSubText(label)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(T3Notifications.contentIntent(this, thread.id))
            .build()
        NotificationManagerCompat.from(this)
            .notify(inputNotificationId(request.id), notification)
    }

    private fun shouldAlert(): Boolean =
        !AppVisibility.foreground && T3Notifications.canPost(this)

    private fun updatePersistent(activeCount: Int = previousActive.size, reconnecting: Boolean = false) {
        if (!foregroundStarted) return
        val text = when {
            reconnecting -> "Reconnecting…"
            activeCount > 0 -> {
                "$activeCount active chat${if (activeCount == 1) "" else "s"}"
            }
            else -> "Connected · waiting for runs"
        }
        val notification = NotificationCompat.Builder(this, T3Notifications.CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.ic_stat_t3)
            .setContentTitle("T3 Code background sync")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(T3Notifications.contentIntent(this))
            .build()
        NotificationManagerCompat.from(this).notify(SERVICE_NOTIFICATION_ID, notification)
    }

    private fun startForegroundCompat() {
        if (foregroundStarted) return
        val notification = NotificationCompat.Builder(this, T3Notifications.CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.ic_stat_t3)
            .setContentTitle("T3 Code background sync")
            .setContentText("Connecting…")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(T3Notifications.contentIntent(this))
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                SERVICE_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(SERVICE_NOTIFICATION_ID, notification)
        }
        foregroundStarted = true
    }

    companion object {
        private const val LOG_TAG = "T3BackgroundSync"
        private const val SERVICE_NOTIFICATION_ID = 7310
        private const val COMPLETION_NOTIFICATION_BASE = 7320
        private const val INPUT_NOTIFICATION_BASE = 7420

        fun start(context: Context) {
            T3Notifications.ensureChannels(context)
            ContextCompat.startForegroundService(
                context,
                Intent(context, BackgroundSyncService::class.java),
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, BackgroundSyncService::class.java))
            NotificationManagerCompat.from(context).cancelAll()
        }

        private fun completionNotificationId(runId: String): Int =
            COMPLETION_NOTIFICATION_BASE + (runId.hashCode() and 0x0FFFFFFF)

        private fun inputNotificationId(requestId: String): Int =
            INPUT_NOTIFICATION_BASE + (requestId.hashCode() and 0x0FFFFFFF)
    }
}

private fun ThreadProjection.notificationBody(): String {
    val text = messages.lastOrNull { it.role == "assistant" && it.text.isNotBlank() }?.text
        ?: visibleTurnItems.lastOrNull {
            it.item.type == "assistant_message" && it.item.text.isNotBlank()
        }?.item?.text
        ?: return "The run finished."
    return text.concludingParagraph()
}

private fun String.concludingParagraph(): String {
    val paragraphs = trim()
        .split(Regex("\\n\\s*\\n"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    val paragraph = (paragraphs.lastOrNull() ?: this)
        .replace(Regex("\\s+"), " ")
        .trim()
    if (paragraph.isBlank()) return "The run finished."
    return if (paragraph.length > 600) {
        paragraph.take(600).trimEnd() + "…"
    } else {
        paragraph
    }
}
