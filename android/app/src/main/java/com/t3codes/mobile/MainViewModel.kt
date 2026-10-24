package com.t3codes.mobile

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.t3codes.core.ServerConfig
import com.t3codes.core.T3Connection
import com.t3codes.core.T3Core
import com.t3codes.core.ThreadShell
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

data class UiState(
    val baseUrl: String = "http://127.0.0.1:3773",
    val token: String = "",
    val pairingLink: String = "",
    val busy: Boolean = false,
    val error: String? = null,
    val config: ServerConfig? = null,
    val projectCount: Int = 0,
    val archivedCount: Int = 0,
    val threads: List<ThreadShell> = emptyList(),
    val log: List<String> = emptyList(),
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("t3_connection", Context.MODE_PRIVATE)
    private var connection: T3Connection? = null

    var state by mutableStateOf(
        UiState(
            baseUrl = prefs.getString(KEY_BASE, "http://127.0.0.1:3773") ?: "http://127.0.0.1:3773",
            token = prefs.getString(KEY_TOKEN, "") ?: "",
        ),
    )
        private set

    fun onBaseUrlChange(value: String) {
        state = state.copy(baseUrl = value)
    }

    fun onTokenChange(value: String) {
        state = state.copy(token = value)
    }

    fun onPairingLinkChange(value: String) {
        state = state.copy(pairingLink = value)
    }

    fun connect() {
        if (state.busy) return
        val base = state.baseUrl.trim()
        val token = state.token.trim()
        if (token.isEmpty()) {
            state = state.copy(error = "Paste a bearer token or pair this device first.")
            return
        }
        state = state.copy(busy = true, error = null)
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    connection?.close()
                    val next = T3Core.connect(base, token)
                    val shell = next.shell()
                    connection = next
                    Triple(next, shell, System.currentTimeMillis())
                }
                val (conn, shell, _) = result
                prefs.edit().putString(KEY_BASE, base).putString(KEY_TOKEN, token).apply()
                state = state.copy(
                    busy = false,
                    config = conn.config,
                    projectCount = shell.projects.size,
                    archivedCount = shell.archivedThreads.size,
                    threads = shell.threads,
                    error = null,
                    log = listOf("connected to ${conn.config.environment.label} (${conn.config.environment.serverVersion})"),
                )
            } catch (error: Throwable) {
                connection?.close()
                connection = null
                state = state.copy(busy = false, error = describe(error))
            }
        }
    }

    fun pair() {
        if (state.busy) return
        val base = state.baseUrl.trim()
        val link = state.pairingLink.trim()
        if (link.isEmpty()) {
            state = state.copy(error = "Paste a pairing link or pairing code.")
            return
        }
        state = state.copy(busy = true, error = null)
        viewModelScope.launch {
            try {
                val token = withContext(Dispatchers.IO) { T3Core.pair(base, link).accessToken }
                prefs.edit().putString(KEY_BASE, base).putString(KEY_TOKEN, token).apply()
                state = state.copy(busy = false, token = token, pairingLink = "", error = null)
                connect()
            } catch (error: Throwable) {
                state = state.copy(busy = false, error = describe(error))
            }
        }
    }

    fun runSelfTest() {
        val conn = connection ?: run {
            state = state.copy(error = "Connect first.")
            return
        }
        if (state.busy) return
        state = state.copy(busy = true, error = null, log = emptyList())
        viewModelScope.launch {
            val lines = mutableListOf<String>()
            try {
                withContext(Dispatchers.IO) {
                    val t0 = System.currentTimeMillis()
                    val shell = conn.shell()
                    lines += "shell: ${shell.projects.size} projects, ${shell.threads.size} threads (${System.currentTimeMillis() - t0} ms)"

                    val shellFeed = conn.watchShell()
                    try {
                        val first = withTimeoutOrNull(10_000) { shellFeed.events.first() }
                        lines += first?.snapshot?.let {
                            "subscribeShell: snapshot seq ${it.snapshotSequence}"
                        } ?: "subscribeShell: no snapshot within 10s"
                    } finally {
                        shellFeed.close()
                    }

                    val thread = shell.threads.firstOrNull()
                    if (thread != null) {
                        val t1 = System.currentTimeMillis()
                        val detail = conn.thread(thread.id)
                        lines += "thread: ${detail.projection.visibleTurnItems.size} visible items, " +
                            "${detail.projection.messages.size} messages (${System.currentTimeMillis() - t1} ms)"

                        val feed = conn.watchThread(thread.id)
                        try {
                            val snapshot = withTimeoutOrNull(10_000) {
                                feed.events.first { it.snapshot != null }.snapshot
                            }
                            lines += if (snapshot != null) {
                                "subscribeThread: snapshot seq ${snapshot.sequence}"
                            } else {
                                "subscribeThread: no snapshot within 10s"
                            }
                        } finally {
                            feed.close()
                        }
                    }

                    if (conn.config.scratchWorkspaceRoot.isNotEmpty()) {
                        runCatching {
                            val status = conn.vcsStatus(conn.config.scratchWorkspaceRoot)
                            lines += "vcs: isRepo=${status.isRepo} ref=${status.refName ?: "-"}"
                        }.onFailure { lines += "vcs: ${it.message}" }
                    }

                    val usage = runCatching { conn.usage() }
                    usage.onSuccess {
                        lines += "usage: ${it.buckets.size} buckets, ${"%.4f".format(it.buckets.sumOf { b -> b.costUsd })} USD"
                    }.onFailure { lines += "usage: ${it.message}" }
                }
            } catch (error: Throwable) {
                lines += "self-test failed: ${describe(error)}"
            }
            state = state.copy(busy = false, log = lines.toList())
        }
    }

    fun refreshThreads() {
        val conn = connection ?: return
        if (state.busy) return
        state = state.copy(busy = true, error = null)
        viewModelScope.launch {
            try {
                val shell = withContext(Dispatchers.IO) { conn.shell() }
                state = state.copy(
                    busy = false,
                    threads = shell.threads,
                    projectCount = shell.projects.size,
                    archivedCount = shell.archivedThreads.size,
                )
            } catch (error: Throwable) {
                state = state.copy(busy = false, error = describe(error))
            }
        }
    }

    fun disconnect() {
        connection?.close()
        connection = null
        state = state.copy(config = null, threads = emptyList(), projectCount = 0, archivedCount = 0, log = emptyList())
    }

    override fun onCleared() {
        connection?.close()
        connection = null
        super.onCleared()
    }

    private fun describe(error: Throwable): String = when (error) {
        is com.t3codes.core.T3Exception ->
            if (error.tag.isNullOrEmpty()) error.message ?: "request failed"
            else "${error.message} (${error.tag})"
        is UnsatisfiedLinkError -> "This build only ships the arm64-v8a core (${error.message})."
        else -> error.message ?: error.javaClass.simpleName
    }

    private companion object {
        const val KEY_BASE = "base_url"
        const val KEY_TOKEN = "token"
    }
}
