package com.t3codes.mobile

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.t3codes.core.Message
import com.t3codes.core.RuntimeRequest
import com.t3codes.core.ServerConfig
import com.t3codes.core.ShellSnapshot
import com.t3codes.core.T3Core
import com.t3codes.core.ThreadDetailSnapshot
import com.t3codes.core.ThreadProjection
import com.t3codes.core.ThreadShell
import com.t3codes.core.TurnItem
import com.t3codes.core.VisibleTurnItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.encodeToJsonElement
import java.io.File
import java.security.MessageDigest

/**
 * Canonical on-device history store.
 *
 * One row per environment/thread is not enough on its own: a thread is a set of
 * independently addressable items (messages, turn items, requests, runs). This
 * store keeps those items in SQLite keyed by their stable id, so an event can
 * update exactly one row without duplicating the transcript. Full snapshots and
 * streamed projections both flow through [saveThread], which diffs payloads
 * before writing and removes items the authoritative projection dropped.
 *
 * The sequence stored with the thread is the resume cursor. A cached thread can
 * therefore be painted immediately and then caught up with `afterSequence`
 * instead of downloading the whole projection again.
 */
data class CachedThread(
    val snapshot: ThreadDetailSnapshot,
    val olderItems: List<VisibleTurnItem> = emptyList(),
)

data class RestoredSession(
    val environmentId: String,
    val config: ServerConfig,
    val shell: ShellSnapshot?,
)

class ClientCache(context: Context) {
    private val appContext = context.applicationContext
    private val database = CacheDatabase(appContext)

    private val legacyRoot = File(appContext.filesDir, "client-cache")

    init {
        runCatching { database.setWriteAheadLoggingEnabled(true) }
    }

    /** Finds the newest cached environment for a base URL without knowing its id yet. */
    fun loadSession(baseUrl: String): RestoredSession? = runCatching {
        val base = normalize(baseUrl)
        runCatching { importLegacySession(base) }
        val environmentId = database.readableDatabase.rawQuery(
            "SELECT environment_id FROM environments WHERE base_url = ? " +
                "ORDER BY updated_at DESC LIMIT 1",
            arrayOf(base),
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: return null
        val config = loadConfig(base, environmentId) ?: return null
        RestoredSession(
            environmentId = environmentId,
            config = config,
            shell = loadShell(base, environmentId),
        )
    }.getOrNull()

    /**
     * Version 0.2.11 kept one JSON file per snapshot. Import the shell eagerly
     * so the first launch after upgrading is not blank, and import each thread
     * lazily the first time it is opened. Thread files are removed only after
     * their contents are readable from the database.
     */
    @Serializable
    private data class LegacyConfig(
        val baseUrl: String = "",
        val environmentId: String = "",
        val config: ServerConfig = ServerConfig(),
    )

    @Serializable
    private data class LegacyShell(
        val baseUrl: String = "",
        val environmentId: String = "",
        val snapshot: ShellSnapshot = ShellSnapshot(),
    )

    @Serializable
    private data class LegacyThread(
        val baseUrl: String = "",
        val environmentId: String = "",
        val threadId: String = "",
        val snapshot: ThreadDetailSnapshot = ThreadDetailSnapshot(),
    )

    private fun importLegacySession(base: String) {
        val directories = legacyRoot.listFiles() ?: return
        val newest = directories.mapNotNull { directory ->
            val file = File(directory, "config.json")
            if (!file.isFile) return@mapNotNull null
            val stored = decodeJson<LegacyConfig>(file.readText()) ?: return@mapNotNull null
            if (normalize(stored.baseUrl) != base || stored.environmentId.isBlank()) {
                return@mapNotNull null
            }
            LegacySessionFile(stored, directory, file.lastModified())
        }.maxByOrNull { it.modifiedAt } ?: return

        val stored = newest.config
        saveConfig(base, stored.environmentId, stored.config)
        val shellFile = File(newest.directory, "shell.json")
        if (shellFile.isFile) {
            val shell = decodeJson<LegacyShell>(shellFile.readText())
            if (shell != null &&
                normalize(shell.baseUrl) == base &&
                shell.environmentId == stored.environmentId
            ) {
                saveShell(base, stored.environmentId, shell.snapshot)
                if (loadShell(base, stored.environmentId) != null) {
                    runCatching { shellFile.delete() }
                }
            }
        }
        if (loadConfig(base, stored.environmentId) != null) {
            runCatching { File(newest.directory, "config.json").delete() }
        }
    }

    private fun importLegacyThread(
        base: String,
        environmentId: String,
        threadId: String,
    ): Boolean {
        if (!legacyRoot.isDirectory) return false
        val directory = legacyEnvironmentDir(base, environmentId)
        val file = File(directory, "thread-${digest(threadId).take(24)}.json")
        if (!file.isFile) return false
        val stored = decodeJson<LegacyThread>(file.readText()) ?: return false
        if (normalize(stored.baseUrl) != base ||
            stored.environmentId != environmentId ||
            stored.threadId != threadId ||
            stored.snapshot.projection.thread.id != threadId
        ) {
            return false
        }
        saveThread(base, environmentId, threadId, stored.snapshot)
        val imported = readThreadFromDb(base, environmentId, threadId) != null
        if (imported) {
            runCatching { file.delete() }
            runCatching {
                if (directory.listFiles().orEmpty().isEmpty()) directory.delete()
            }
        }
        return imported
    }

    private data class LegacySessionFile(
        val config: LegacyConfig,
        val directory: File,
        val modifiedAt: Long,
    )

    fun loadConfig(baseUrl: String, environmentId: String): ServerConfig? = runCatching {
        database.readableDatabase.rawQuery(
            "SELECT config_json FROM environments " +
                "WHERE base_url = ? AND environment_id = ?",
            arrayOf(normalize(baseUrl), environmentId),
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            val config = T3Core.json.decodeFromString<ServerConfig>(cursor.getString(0))
            config.takeIf {
                it.cwd.isNotBlank() || it.environment.environmentId.isNotBlank()
            }
        }
    }.getOrNull()

    fun saveConfig(baseUrl: String, environmentId: String, config: ServerConfig) {
        if (environmentId.isBlank()) return
        runCatching {
            val db = database.writableDatabase
            val now = System.currentTimeMillis()
            db.beginTransaction()
            try {
                db.insertWithOnConflict(
                    "environments",
                    null,
                    ContentValues().apply {
                        put("base_url", normalize(baseUrl))
                        put("environment_id", environmentId)
                        put("config_json", T3Core.json.encodeToString(ServerConfig.serializer(), config))
                        put("updated_at", now)
                    },
                    SQLiteDatabase.CONFLICT_REPLACE,
                )
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            pruneEnvironments(db)
        }
    }

    fun loadShell(baseUrl: String, environmentId: String): ShellSnapshot? = runCatching {
        database.readableDatabase.rawQuery(
            "SELECT snapshot_json FROM shells WHERE base_url = ? AND environment_id = ?",
            arrayOf(normalize(baseUrl), environmentId),
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            val shell = T3Core.json.decodeFromString<ShellSnapshot>(cursor.getString(0))
            shell.takeIf {
                it.threads.isNotEmpty() || it.archivedThreads.isNotEmpty() || it.projects.isNotEmpty()
            }
        }
    }.getOrNull()

    fun saveShell(baseUrl: String, environmentId: String, snapshot: ShellSnapshot) {
        if (environmentId.isBlank()) return
        runCatching {
            val db = database.writableDatabase
            val base = normalize(baseUrl)
            val now = System.currentTimeMillis()
            db.beginTransaction()
            try {
                db.insertWithOnConflict(
                    "shells",
                    null,
                    ContentValues().apply {
                        put("base_url", base)
                        put("environment_id", environmentId)
                        put("snapshot_json", T3Core.json.encodeToString(ShellSnapshot.serializer(), snapshot))
                        put("snapshot_sequence", snapshot.snapshotSequence)
                        put("updated_at", now)
                    },
                    SQLiteDatabase.CONFLICT_REPLACE,
                )
                db.update(
                    "environments",
                    ContentValues().apply { put("updated_at", now) },
                    "base_url = ? AND environment_id = ?",
                    arrayOf(base, environmentId),
                )
                // Shell snapshots are authoritative for summaries, but not for
                // the transcript. Only refresh rows we already hold history for.
                (snapshot.threads + snapshot.archivedThreads).forEach { summary ->
                    db.update(
                        "threads",
                        ContentValues().apply {
                            put("summary_json", T3Core.json.encodeToString(ThreadShell.serializer(), summary))
                            put("archived", if (summary.isArchived) 1 else 0)
                        },
                        "base_url = ? AND environment_id = ? AND thread_id = ?",
                        arrayOf(base, environmentId, summary.id),
                    )
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }

    fun loadThread(baseUrl: String, environmentId: String, threadId: String): ThreadDetailSnapshot? =
        loadThreadWithHistory(baseUrl, environmentId, threadId)?.snapshot

    fun loadThreadWithHistory(
        baseUrl: String,
        environmentId: String,
        threadId: String,
    ): CachedThread? = runCatching {
        val base = normalize(baseUrl)
        readThreadFromDb(base, environmentId, threadId) ?: run {
            if (importLegacyThread(base, environmentId, threadId)) {
                readThreadFromDb(base, environmentId, threadId)
            } else {
                null
            }
        }
    }.getOrNull()

    private fun readThreadFromDb(
        base: String,
        environmentId: String,
        threadId: String,
    ): CachedThread? = runCatching {
        val db = database.readableDatabase
        val row = db.rawQuery(
            "SELECT snapshot_sequence, history_cursor, has_more_history, " +
                "latest_local_turn_ordinal, projection_updated_at FROM threads " +
                "WHERE base_url = ? AND environment_id = ? AND thread_id = ?",
            arrayOf(base, environmentId, threadId),
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            StoredThreadRow(
                sequence = cursor.getLong(0),
                historyCursor = if (cursor.isNull(1)) null else cursor.getString(1),
                hasMoreHistory = cursor.getInt(2) != 0,
                latestLocalTurnOrdinal = if (cursor.isNull(3)) null else cursor.getInt(3),
                projectionUpdatedAt = cursor.getString(4),
            )
        }

        val items = mutableMapOf<String, MutableList<StoredItem>>()
        db.rawQuery(
            "SELECT kind, ordinal, payload_json FROM thread_items " +
                "WHERE base_url = ? AND environment_id = ? AND thread_id = ? " +
                "ORDER BY kind, ordinal, item_id",
            arrayOf(base, environmentId, threadId),
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val kind = cursor.getString(0)
                items.getOrPut(kind) { mutableListOf() }
                    .add(StoredItem(cursor.getInt(1), cursor.getString(2)))
            }
        }

        val projection = projectionFrom(items) ?: return null
        if (projection.thread.id.isBlank()) return null
        CachedThread(
            snapshot = ThreadDetailSnapshot(
                snapshotSequence = row.sequence,
                projection = projection.copy(updatedAt = row.projectionUpdatedAt),
                historyCursor = row.historyCursor,
                hasMoreHistory = row.hasMoreHistory,
                latestLocalTurnOrdinal = row.latestLocalTurnOrdinal,
            ),
            olderItems = items["older"].orEmpty()
                .mapNotNull { item -> decodeJson<VisibleTurnItem>(item.payload) }
                .sortedBy { it.position },
        )
    }.getOrNull()

    fun saveThread(
        baseUrl: String,
        environmentId: String,
        threadId: String,
        snapshot: ThreadDetailSnapshot,
        olderItems: List<VisibleTurnItem>? = null,
    ) {
        if (environmentId.isBlank() || threadId.isBlank()) return
        if (snapshot.projection.thread.id.isBlank()) return
        runCatching {
            val db = database.writableDatabase
            val base = normalize(baseUrl)
            db.beginTransaction()
            try {
                val existing = db.rawQuery(
                    "SELECT snapshot_sequence, history_cursor, has_more_history, " +
                        "latest_local_turn_ordinal FROM threads " +
                        "WHERE base_url = ? AND environment_id = ? AND thread_id = ?",
                    arrayOf(base, environmentId, threadId),
                ).use { cursor ->
                    if (cursor.moveToFirst()) {
                        StoredThreadRow(
                            sequence = cursor.getLong(0),
                            historyCursor = if (cursor.isNull(1)) null else cursor.getString(1),
                            hasMoreHistory = cursor.getInt(2) != 0,
                            latestLocalTurnOrdinal = if (cursor.isNull(3)) null else cursor.getInt(3),
                        )
                    } else {
                        null
                    }
                }

                // A late event must never roll the transcript back. Equal
                // sequences are accepted because token updates reuse them.
                if (existing != null && snapshot.snapshotSequence < existing.sequence) return

                val rows = buildList {
                    addItem("thread", snapshot.projection.thread.id, 0, snapshot.projection.updatedAt,
                        snapshot.projection.thread)
                    snapshot.projection.runs.forEachIndexed { index, item ->
                        addItem("run", item.id(), index, item.updatedAt(), item)
                    }
                    snapshot.projection.attempts.forEachIndexed { index, item ->
                        addItem("attempt", item.id(), index, item.updatedAt(), item)
                    }
                    snapshot.projection.nodes.forEachIndexed { index, item ->
                        addItem("node", item.id(), index, item.updatedAt(), item)
                    }
                    snapshot.projection.subagents.forEachIndexed { index, item ->
                        addItem("subagent", item.id(), index, item.updatedAt(), item)
                    }
                    snapshot.projection.providerSessions.forEachIndexed { index, item ->
                        addItem("provider_session", item.id(), index, item.updatedAt(), item)
                    }
                    snapshot.projection.providerThreads.forEachIndexed { index, item ->
                        addItem("provider_thread", item.id(), index, item.updatedAt(), item)
                    }
                    snapshot.projection.providerTurns.forEachIndexed { index, item ->
                        addItem("provider_turn", item.id(), index, item.updatedAt(), item)
                    }
                    snapshot.projection.runtimeRequests.forEachIndexed { index, item ->
                        addItem("runtime_request", item.id.ifBlank { "request:$index" }, index, item.createdAt, item)
                    }
                    snapshot.projection.messages.forEachIndexed { index, item ->
                        addItem("message", item.id.ifBlank { "message:$index" }, index, item.updatedAt, item)
                    }
                    snapshot.projection.plans.forEachIndexed { index, item ->
                        addItem("plan", item.id(), index, item.updatedAt(), item)
                    }
                    snapshot.projection.turnItems.forEachIndexed { index, item ->
                        addItem("turn_item", item.id.ifBlank { "turn:$index" }, index, item.updatedAt, item)
                    }
                    snapshot.projection.checkpointScopes.forEachIndexed { index, item ->
                        addItem("checkpoint_scope", item.id(), index, item.updatedAt(), item)
                    }
                    snapshot.projection.checkpoints.forEachIndexed { index, item ->
                        addItem("checkpoint", item.id(), index, item.updatedAt(), item)
                    }
                    snapshot.projection.contextHandoffs.forEachIndexed { index, item ->
                        addItem("context_handoff", item.id(), index, item.updatedAt(), item)
                    }
                    snapshot.projection.contextTransfers.forEachIndexed { index, item ->
                        addItem("context_transfer", item.id(), index, item.updatedAt(), item)
                    }
                    snapshot.projection.visibleTurnItems.forEachIndexed { index, row ->
                        addItem(
                            "visible",
                            row.item.id.ifBlank { row.sourceItemId }.ifBlank { "row:$index" },
                            row.position,
                            row.item.updatedAt,
                            row,
                        )
                    }
                    olderItems?.forEachIndexed { index, row ->
                        addItem(
                            "older",
                            row.item.id.ifBlank { row.sourceItemId }.ifBlank { "older:$index" },
                            index,
                            row.item.updatedAt,
                            row,
                        )
                    }
                }

                val kinds = rows.mapTo(mutableSetOf()) { it.kind }
                val incoming = rows.associateBy { it.kind to it.id }
                val existingPayloads: Map<Pair<String, String>, ItemState> =
                    if (kinds.isEmpty()) {
                        emptyMap()
                    } else {
                        val placeholders = kinds.joinToString(",") { "?" }
                        db.rawQuery(
                            "SELECT kind, item_id, payload_hash, ordinal FROM thread_items " +
                                "WHERE base_url = ? AND environment_id = ? AND thread_id = ? " +
                                "AND kind IN ($placeholders)",
                            arrayOf(base, environmentId, threadId) + kinds.toTypedArray(),
                        ).use { cursor ->
                            buildMap {
                                while (cursor.moveToNext()) {
                                    put(
                                        cursor.getString(0) to cursor.getString(1),
                                        ItemState(cursor.getString(2), cursor.getInt(3)),
                                    )
                                }
                            }
                        }
                    }

                var changed = false
                rows.forEach { item ->
                    val stored = existingPayloads[item.kind to item.id]
                    if (stored != null && stored.hash == item.hash && stored.ordinal == item.ordinal) {
                        return@forEach
                    }
                    db.insertWithOnConflict(
                        "thread_items",
                        null,
                        ContentValues().apply {
                            put("base_url", base)
                            put("environment_id", environmentId)
                            put("thread_id", threadId)
                            put("kind", item.kind)
                            put("item_id", item.id)
                            put("ordinal", item.ordinal)
                            put("payload_hash", item.hash)
                            put("payload_json", item.payload)
                        },
                        SQLiteDatabase.CONFLICT_REPLACE,
                    )
                    changed = true
                }
                existingPayloads.keys.forEach { key ->
                    if (incoming[key] == null) {
                        db.delete(
                            "thread_items",
                            "base_url = ? AND environment_id = ? AND thread_id = ? " +
                                "AND kind = ? AND item_id = ?",
                            arrayOf(base, environmentId, threadId, key.first, key.second),
                        )
                        changed = true
                    }
                }

                val summary = T3Core.json.encodeToString(
                    ThreadShell.serializer(),
                    snapshot.projection.thread,
                )
                val cursor = snapshot.historyCursor ?: existing?.historyCursor
                val hasMore = snapshot.hasMoreHistory ?: existing?.hasMoreHistory ?: false
                val latestLocal = snapshot.latestLocalTurnOrdinal ?: existing?.latestLocalTurnOrdinal
                val values = ContentValues().apply {
                    put("base_url", base)
                    put("environment_id", environmentId)
                    put("thread_id", threadId)
                    put("summary_json", summary)
                    put("snapshot_sequence", snapshot.snapshotSequence)
                    if (cursor == null) putNull("history_cursor") else put("history_cursor", cursor)
                    put("has_more_history", if (hasMore) 1 else 0)
                    if (latestLocal == null) putNull("latest_local_turn_ordinal") else put(
                        "latest_local_turn_ordinal",
                        latestLocal,
                    )
                    put("projection_updated_at", snapshot.projection.updatedAt)
                    put("archived", if (snapshot.projection.thread.isArchived) 1 else 0)
                    put("updated_at", System.currentTimeMillis())
                }
                db.insertWithOnConflict(
                    "threads",
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE,
                )
                if (changed) pruneThread(db, base, environmentId, threadId)
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            pruneThreads(db, base, environmentId)
        }
    }

    fun removeThread(baseUrl: String, environmentId: String, threadId: String) {
        runCatching {
            val db = database.writableDatabase
            val base = normalize(baseUrl)
            db.beginTransaction()
            try {
                db.delete(
                    "thread_items",
                    "base_url = ? AND environment_id = ? AND thread_id = ?",
                    arrayOf(base, environmentId, threadId),
                )
                db.delete(
                    "threads",
                    "base_url = ? AND environment_id = ? AND thread_id = ?",
                    arrayOf(base, environmentId, threadId),
                )
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }

    fun clearThreads(baseUrl: String, environmentId: String) {
        runCatching {
            val db = database.writableDatabase
            val base = normalize(baseUrl)
            db.beginTransaction()
            try {
                db.delete(
                    "thread_items",
                    "base_url = ? AND environment_id = ?",
                    arrayOf(base, environmentId),
                )
                db.delete(
                    "threads",
                    "base_url = ? AND environment_id = ?",
                    arrayOf(base, environmentId),
                )
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }

    /** Removes every cached environment. Used when the app session is reset. */
    fun clearAll() {
        runCatching {
            val db = database.writableDatabase
            db.beginTransaction()
            try {
                db.delete("thread_items", null, null)
                db.delete("threads", null, null)
                db.delete("shells", null, null)
                db.delete("environments", null, null)
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }

    fun close() {
        runCatching { database.close() }
    }

    // ------------------------------------------------------------------ helpers

    private fun projectionFrom(items: Map<String, List<StoredItem>>): ThreadProjection? {
        fun <T> decodeList(kind: String, decode: (String) -> T?): List<T> =
            items[kind].orEmpty().mapNotNull { decode(it.payload) }

        val thread = items["thread"]?.firstOrNull()
            ?.let { decodeJson<ThreadShell>(it.payload) }
            ?: return null
        return ThreadProjection(
            thread = thread,
            runs = items["run"].orEmpty().map { it.element() },
            attempts = items["attempt"].orEmpty().map { it.element() },
            nodes = items["node"].orEmpty().map { it.element() },
            subagents = items["subagent"].orEmpty().map { it.element() },
            providerSessions = items["provider_session"].orEmpty().map { it.element() },
            providerThreads = items["provider_thread"].orEmpty().map { it.element() },
            providerTurns = items["provider_turn"].orEmpty().map { it.element() },
            runtimeRequests = decodeList("runtime_request") { decodeJson<RuntimeRequest>(it) },
            messages = decodeList("message") { decodeJson<Message>(it) },
            plans = items["plan"].orEmpty().map { it.element() },
            turnItems = decodeList("turn_item") { decodeJson<TurnItem>(it) },
            checkpointScopes = items["checkpoint_scope"].orEmpty().map { it.element() },
            checkpoints = items["checkpoint"].orEmpty().map { it.element() },
            contextHandoffs = items["context_handoff"].orEmpty().map { it.element() },
            contextTransfers = items["context_transfer"].orEmpty().map { it.element() },
            visibleTurnItems = decodeList("visible") { decodeJson<VisibleTurnItem>(it) }
                .sortedBy { it.position },
            updatedAt = thread.updatedAt,
        )
    }

    private fun pruneThread(db: SQLiteDatabase, base: String, environmentId: String, threadId: String) {
        // Keep older history bounded. Visible/turn items are the live timeline;
        // expanded pages are the only category that can grow without limit.
        val overflow = db.rawQuery(
            "SELECT COUNT(*) FROM thread_items WHERE base_url = ? AND environment_id = ? " +
                "AND thread_id = ? AND kind = 'older'",
            arrayOf(base, environmentId, threadId),
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else 0 } - MAX_OLDER_ITEMS
        if (overflow <= 0) return
        db.execSQL(
            "DELETE FROM thread_items WHERE rowid IN (" +
                "SELECT rowid FROM thread_items WHERE base_url = ? AND environment_id = ? " +
                "AND thread_id = ? AND kind = 'older' ORDER BY ordinal DESC LIMIT ?)",
            arrayOf(base, environmentId, threadId, overflow.toString()),
        )
    }

    private fun pruneThreads(db: SQLiteDatabase, base: String, environmentId: String) {
        val stale = db.rawQuery(
            "SELECT thread_id FROM threads WHERE base_url = ? AND environment_id = ? " +
                "ORDER BY updated_at DESC LIMIT -1 OFFSET ?",
            arrayOf(base, environmentId, MAX_THREADS.toString()),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.getString(0))
            }
        }
        stale.forEach { threadId ->
            db.delete(
                "thread_items",
                "base_url = ? AND environment_id = ? AND thread_id = ?",
                arrayOf(base, environmentId, threadId),
            )
            db.delete(
                "threads",
                "base_url = ? AND environment_id = ? AND thread_id = ?",
                arrayOf(base, environmentId, threadId),
            )
        }
    }

    private fun pruneEnvironments(db: SQLiteDatabase) {
        val stale = db.rawQuery(
            "SELECT base_url, environment_id FROM environments " +
                "ORDER BY updated_at DESC LIMIT -1 OFFSET ?",
            arrayOf(MAX_ENVIRONMENTS.toString()),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.getString(0) to cursor.getString(1))
            }
        }
        stale.forEach { (base, environmentId) ->
            db.delete(
                "thread_items",
                "base_url = ? AND environment_id = ?",
                arrayOf(base, environmentId),
            )
            db.delete(
                "threads",
                "base_url = ? AND environment_id = ?",
                arrayOf(base, environmentId),
            )
            db.delete(
                "shells",
                "base_url = ? AND environment_id = ?",
                arrayOf(base, environmentId),
            )
            db.delete(
                "environments",
                "base_url = ? AND environment_id = ?",
                arrayOf(base, environmentId),
            )
        }
    }

    private fun normalize(baseUrl: String): String = baseUrl.trim().trimEnd('/')

    private fun legacyEnvironmentDir(base: String, environmentId: String): File =
        File(legacyRoot, digest("$base|$environmentId").take(20))

    private fun digest(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }

    private inline fun <reified T> decodeJson(payload: String): T? =
        runCatching { T3Core.json.decodeFromString<T>(payload) }.getOrNull()

    private data class StoredThreadRow(
        val sequence: Long,
        val historyCursor: String?,
        val hasMoreHistory: Boolean,
        val latestLocalTurnOrdinal: Int? = null,
        val projectionUpdatedAt: String = "",
    )

    private data class StoredItem(val ordinal: Int, val payload: String) {
        fun element(): JsonElement = T3Core.json.parseToJsonElement(payload)
    }

    private data class ItemRow(
        val kind: String,
        val id: String,
        val ordinal: Int,
        val payload: String,
    ) {
        val hash: String = "${payload.length}:${payload.hashCode()}"
    }

    private data class ItemState(val hash: String, val ordinal: Int)

    private inline fun <reified T> MutableList<ItemRow>.addItem(
        kind: String,
        id: String,
        ordinal: Int,
        updatedAt: String,
        value: T,
    ) {
        val payload = runCatching { T3Core.json.encodeToJsonElement(value).toString() }
            .getOrNull() ?: return
        add(ItemRow(kind, id, ordinal, payload))
    }

    private fun JsonElement.id(): String =
        ((this as? JsonObject)?.get("id") as? JsonPrimitive)?.contentOrNull.orEmpty()

    private fun JsonElement.updatedAt(): String =
        ((this as? JsonObject)?.get("updatedAt") as? JsonPrimitive)?.contentOrNull.orEmpty()

    private class CacheDatabase(context: Context) :
        SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE environments (
                    base_url TEXT NOT NULL,
                    environment_id TEXT NOT NULL,
                    config_json TEXT NOT NULL,
                    updated_at INTEGER NOT NULL,
                    PRIMARY KEY (base_url, environment_id)
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE shells (
                    base_url TEXT NOT NULL,
                    environment_id TEXT NOT NULL,
                    snapshot_json TEXT NOT NULL,
                    snapshot_sequence INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    PRIMARY KEY (base_url, environment_id)
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE threads (
                    base_url TEXT NOT NULL,
                    environment_id TEXT NOT NULL,
                    thread_id TEXT NOT NULL,
                    summary_json TEXT NOT NULL,
                    snapshot_sequence INTEGER NOT NULL,
                    history_cursor TEXT,
                    has_more_history INTEGER NOT NULL,
                    latest_local_turn_ordinal INTEGER,
                    projection_updated_at TEXT NOT NULL,
                    archived INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    PRIMARY KEY (base_url, environment_id, thread_id)
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE thread_items (
                    base_url TEXT NOT NULL,
                    environment_id TEXT NOT NULL,
                    thread_id TEXT NOT NULL,
                    kind TEXT NOT NULL,
                    item_id TEXT NOT NULL,
                    ordinal INTEGER NOT NULL,
                    payload_hash TEXT NOT NULL,
                    payload_json TEXT NOT NULL,
                    PRIMARY KEY (base_url, environment_id, thread_id, kind, item_id)
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX thread_items_kind ON thread_items " +
                    "(base_url, environment_id, thread_id, kind, ordinal)",
            )
            db.execSQL("CREATE INDEX threads_updated ON threads (base_url, environment_id, updated_at)")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS thread_items")
            db.execSQL("DROP TABLE IF EXISTS threads")
            db.execSQL("DROP TABLE IF EXISTS shells")
            db.execSQL("DROP TABLE IF EXISTS environments")
            onCreate(db)
        }
    }

    private companion object {
        const val DATABASE_NAME = "t3-client-cache.db"
        const val DATABASE_VERSION = 1
        const val MAX_THREADS = 80
        const val MAX_ENVIRONMENTS = 12
        const val MAX_OLDER_ITEMS = 1_500
    }
}
