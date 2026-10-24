package com.t3codes.mobile

import androidx.compose.ui.graphics.Color
import com.t3codes.core.ModelSelection
import com.t3codes.core.ThreadProjection
import com.t3codes.core.ThreadShell
import com.t3codes.core.TurnItem
import com.t3codes.core.VisibleTurnItem
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class TimelineKind {
    User,
    Assistant,
    Reasoning,
    Command,
    FileChange,
    Tool,
    Notice,
    Checkpoint,
    Unknown,
}

data class TimelineEntry(
    val key: String,
    val kind: TimelineKind,
    val item: TurnItem,
    val text: String = "",
    val secondary: String = "",
    val command: String = "",
    val output: String = "",
    val status: String = "",
    val exitCode: Int? = null,
    val streaming: Boolean = false,
    val attachmentCount: Int = 0,
)

/**
 * A rendered block in the feed. Consecutive low-level work items collapse into
 * one [WorkGroup]; conversation turns stay as [Single] items.
 */
sealed interface TimelineBlock {
    val key: String

    data class Single(val entry: TimelineEntry) : TimelineBlock {
        override val key: String get() = entry.key
    }

    data class WorkGroup(
        override val key: String,
        val entries: List<TimelineEntry>,
    ) : TimelineBlock
}

private val workEntryKinds = setOf(
    TimelineKind.Reasoning,
    TimelineKind.Command,
    TimelineKind.FileChange,
    TimelineKind.Tool,
)

fun groupTimeline(entries: List<TimelineEntry>): List<TimelineBlock> {
    val blocks = mutableListOf<TimelineBlock>()
    val work = mutableListOf<TimelineEntry>()
    fun flushWork() {
        if (work.isEmpty()) return
        if (work.size == 1) {
            blocks += TimelineBlock.Single(work.first())
        } else {
            blocks += TimelineBlock.WorkGroup(
                key = "work-${work.first().key}",
                entries = work.toList(),
            )
        }
        work.clear()
    }
    entries.forEach { entry ->
        if (entry.kind in workEntryKinds) {
            work += entry
        } else {
            flushWork()
            blocks += TimelineBlock.Single(entry)
        }
    }
    flushWork()
    return blocks
}

enum class WorkAction { Thought, Read, Search, Command, Edit, Browse, Tool }

fun workAction(entry: TimelineEntry): WorkAction = when (entry.kind) {
    TimelineKind.Reasoning -> WorkAction.Thought
    TimelineKind.Command -> WorkAction.Command
    TimelineKind.FileChange -> WorkAction.Edit
    else -> {
        val name = "${entry.item.toolName} ${entry.text} ${entry.secondary}".lowercase(Locale.US)
        when {
            listOf("read", "open_file", "view_file", "cat ").any { name.contains(it) } -> WorkAction.Read
            listOf("search", "grep", "find", "glob", "list_dir", "code_search")
                .any { name.contains(it) } -> WorkAction.Search
            listOf("fetch", "browser", "web", "http", "url").any { name.contains(it) } ->
                WorkAction.Browse
            listOf("edit", "write", "patch", "apply", "replace").any { name.contains(it) } ->
                WorkAction.Edit
            else -> WorkAction.Tool
        }
    }
}

fun workGroupSummary(entries: List<TimelineEntry>): String {
    val counts = entries.groupingBy(::workAction).eachCount()
    val parts = buildList {
        if (counts[WorkAction.Thought] != null) add("Thought")
        counts[WorkAction.Read]?.let { add("Read $it file${if (it == 1) "" else "s"}") }
        counts[WorkAction.Search]?.let { add("Searched $it time${if (it == 1) "" else "s"}") }
        counts[WorkAction.Command]?.let { add("Ran $it command${if (it == 1) "" else "s"}") }
        counts[WorkAction.Edit]?.let { add("Changed $it file${if (it == 1) "" else "s"}") }
        counts[WorkAction.Browse]?.let { add("Browsed $it page${if (it == 1) "" else "s"}") }
        counts[WorkAction.Tool]?.let { add("Used $it tool${if (it == 1) "" else "s"}") }
    }
    if (parts.isEmpty()) return "Work"
    val headline = upperFirst(parts.first())
    return when (parts.size) {
        1 -> headline
        2 -> "$headline and ${lowerFirst(parts[1])}"
        else -> buildString {
            append(headline)
            append(", ")
            append(parts.drop(1).dropLast(1).joinToString(", ") { lowerFirst(it) })
            append(" and ")
            append(lowerFirst(parts.last()))
        }
    }
}

private fun upperFirst(value: String): String =
    if (value.isEmpty()) value else value[0].uppercaseChar() + value.substring(1)

private fun lowerFirst(value: String): String =
    if (value.isEmpty()) value else value[0].lowercaseChar() + value.substring(1)

fun workEntryLabel(entry: TimelineEntry): String = when (entry.kind) {
    TimelineKind.Reasoning -> if (entry.streaming) "Thinking…" else "Thought"
    TimelineKind.Command -> {
        val command = entry.command.lineSequence().firstOrNull().orEmpty().trim()
        if (command.isBlank()) "Command" else command.take(96)
    }
    TimelineKind.FileChange -> {
        val name = entry.text.substringAfterLast('/').ifBlank { entry.text.ifBlank { "File" } }
        "Edited $name"
    }
    else -> entry.text
        .replace(Regex("\\s+(?:complete|completed)\\s*$", RegexOption.IGNORE_CASE), "")
        .trim()
        .ifBlank { entry.item.toolName.ifBlank { "Tool" } }
}

fun ThreadProjection.timeline(
    older: List<VisibleTurnItem> = emptyList(),
    pending: List<VisibleTurnItem> = emptyList(),
): List<TimelineEntry> {
    val base = if (older.isNotEmpty() || visibleTurnItems.isNotEmpty()) {
        (older + visibleTurnItems).mapIndexedNotNull { index, value -> value.item.toEntry(index) }
    } else {
        messages.mapIndexedNotNull { index, message ->
            if (message.text.isBlank()) return@mapIndexedNotNull null
            TimelineEntry(
                key = "message-${message.id.ifEmpty { index.toString() }}",
                kind = if (message.role == "user") TimelineKind.User else TimelineKind.Assistant,
                item = TurnItem(
                    id = message.id,
                    type = if (message.role == "user") "user_message" else "assistant_message",
                    text = message.text,
                    attachments = message.attachments,
                ),
                text = message.text,
                attachmentCount = message.attachments.size,
            )
        }
    }
    return base + pending.mapIndexedNotNull { index, value -> value.item.toEntry(base.size + index) }
}

private val unfinishedRunStatuses = setOf("preparing", "queued", "starting", "running", "waiting")

/** True while the projection still has an unfinished run. */
fun ThreadProjection.isRunning(): Boolean = runs.any { run ->
    val status = (run as? JsonObject)
        ?.get("status")
        ?.let { (it as? JsonPrimitive)?.contentOrNull }
        .orEmpty()
    status in unfinishedRunStatuses
}

/** The run the Stop button should interrupt, if one is active. */
fun ThreadProjection.activeRunId(summary: ThreadShell? = null): String? {
    thread.activeRunId?.takeIf { it.isNotBlank() }?.let { return it }
    summary?.activeRunId?.takeIf { it.isNotBlank() }?.let { return it }
    // Older projections can report a running status without activeRunId; the
    // latest run is then the one the Stop button means.
    if (thread.status == "running") {
        thread.latestRunId?.takeIf { it.isNotBlank() }?.let { return it }
    }
    if (summary?.status == "running") {
        summary.latestRunId?.takeIf { it.isNotBlank() }?.let { return it }
    }
    for (run in runs.asReversed()) {
        val item = run as? JsonObject ?: continue
        val status = (item["status"] as? JsonPrimitive)?.contentOrNull.orEmpty()
        if (status !in unfinishedRunStatuses) continue
        (item["id"] as? JsonPrimitive)?.contentOrNull
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }
    }
    return null
}

private fun TurnItem.toEntry(index: Int): TimelineEntry? {
    val entryKey = id.ifEmpty { "$type-$ordinal-$index" }
    return when (type) {
        "user_message", "user-message" -> TimelineEntry(
            key = entryKey,
            kind = TimelineKind.User,
            item = this,
            text = text.ifBlank { message },
            attachmentCount = attachments.size,
        )

        "assistant_message", "assistant-message" -> {
            if (text.isBlank() && streaming != true) null
            else TimelineEntry(
                key = entryKey,
                kind = TimelineKind.Assistant,
                item = this,
                text = text,
                streaming = streaming == true,
            )
        }

        "reasoning" -> {
            if (text.isBlank()) null
            else TimelineEntry(
                key = entryKey,
                kind = TimelineKind.Reasoning,
                item = this,
                text = text,
                streaming = streaming == true,
            )
        }

        "command_execution" -> TimelineEntry(
            key = entryKey,
            kind = TimelineKind.Command,
            item = this,
            command = input.asDisplayText().ifBlank { message },
            output = output.commandOutput(),
            status = status,
            exitCode = exitCode,
            streaming = streaming == true,
        )

        "file_change" -> TimelineEntry(
            key = entryKey,
            kind = TimelineKind.FileChange,
            item = this,
            text = fileName.ifBlank { title },
            secondary = input.asDisplayText().take(240),
            status = status,
        )

        "dynamic_tool", "tool_call" -> TimelineEntry(
            key = entryKey,
            kind = TimelineKind.Tool,
            item = this,
            text = title.ifBlank { toolName.ifBlank { "Tool" } },
            secondary = toolName,
            command = input.asDisplayText().take(600),
            output = output.commandOutput().take(2000),
            status = status,
            streaming = streaming == true,
        )

        "checkpoint" -> TimelineEntry(
            key = entryKey,
            kind = TimelineKind.Checkpoint,
            item = this,
            text = title.ifBlank { "Checkpoint" },
        )

        "run_interrupt_request", "run_interrupt_result", "plan", "context_handoff" -> TimelineEntry(
            key = entryKey,
            kind = TimelineKind.Notice,
            item = this,
            text = message.ifBlank { text }.ifBlank { title }.ifBlank { type.replace('_', ' ') },
            status = status,
        )

        else -> {
            val body = text.ifBlank { message }.ifBlank { title }
            if (body.isBlank()) null
            else TimelineEntry(
                key = entryKey,
                kind = TimelineKind.Unknown,
                item = this,
                text = body,
                secondary = type.replace('_', ' '),
                status = status,
            )
        }
    }
}

fun JsonElement?.asDisplayText(): String = when (this) {
    null, is JsonNull -> ""
    is JsonPrimitive -> content
    is JsonArray -> joinToString("\n") { it.asDisplayText() }
    is JsonObject -> entries.joinToString("\n") { (key, value) ->
        val rendered = value.asDisplayText().replace("\n", " ")
        if (rendered.isBlank()) key else "$key: $rendered"
    }
    else -> toString()
}

private fun JsonElement?.commandOutput(): String {
    val element = this ?: return ""
    if (element !is JsonObject) return element.asDisplayText()
    val preferred = listOf("output", "stdout", "text", "content", "result")
        .firstNotNullOfOrNull { key -> (element[key] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() } }
    if (!preferred.isNullOrBlank()) {
        val stderr = (element["stderr"] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
        return if (stderr.isNullOrBlank()) preferred else "$preferred\n$stderr"
    }
    return element.asDisplayText()
}

// ------------------------------------------------------------------- formatting

fun relativeTime(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    val instant = runCatching { Instant.parse(iso) }.getOrNull() ?: return ""
    val duration = Duration.between(instant, Instant.now())
    val minutes = duration.toMinutes()
    return when {
        minutes < 1 -> "now"
        minutes < 60 -> "${minutes}m"
        minutes < 60 * 24 -> "${minutes / 60}h"
        minutes < 60 * 24 * 7 -> "${minutes / (60 * 24)}d"
        else -> DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
            .withZone(ZoneId.systemDefault())
            .format(instant)
    }
}

fun formatCost(value: Double): String = when {
    value <= 0.0 -> "$0.00"
    value < 0.01 -> "$%.4f".format(Locale.US, value)
    value < 10.0 -> "$%.3f".format(Locale.US, value)
    else -> "$%.2f".format(Locale.US, value)
}

fun formatTokens(value: Long): String = when {
    value >= 1_000_000 -> "%.1fM".format(Locale.US, value / 1_000_000.0)
    value >= 1_000 -> "%.1fk".format(Locale.US, value / 1_000.0)
    else -> value.toString()
}

fun modelName(selection: ModelSelection?): String {
    val model = selection?.model.orEmpty()
    return model.substringAfterLast('/').ifBlank { "default model" }
}

fun modelLabel(thread: ThreadShell): String = modelName(thread.modelSelection)

fun threadStatus(thread: ThreadShell?): StatusInfo {
    if (thread == null) return StatusInfo("Ready", StatusTone.Neutral)
    return when {
        thread.lastError != null -> StatusInfo("Error", StatusTone.Error)
        thread.pendingRuntimeRequest != null -> StatusInfo("Needs you", StatusTone.Attention)
        thread.activeRunId != null || thread.status == "running" -> StatusInfo("Working", StatusTone.Active)
        thread.isArchived -> StatusInfo("Archived", StatusTone.Neutral)
        thread.settledAt != null -> StatusInfo("Done", StatusTone.Neutral)
        else -> StatusInfo("Ready", StatusTone.Neutral)
    }
}

enum class StatusTone { Neutral, Active, Attention, Error }

data class StatusInfo(val label: String, val tone: StatusTone)

fun initials(value: String): String {
    val words = value.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        words.isEmpty() -> "T3"
        words.size == 1 -> words.first().take(2).uppercase(Locale.getDefault())
        else -> (words[0].take(1) + words[1].take(1)).uppercase(Locale.getDefault())
    }
}

private val avatarPalette = listOf(
    Color(0xFF7C5CFF),
    Color(0xFF2F9E8F),
    Color(0xFFD1723A),
    Color(0xFF3D7DD8),
    Color(0xFFB34F8C),
    Color(0xFF5C8A3A),
)

fun avatarColor(key: String): Color =
    avatarPalette[(key.hashCode() and 0x7fffffff) % avatarPalette.size]
