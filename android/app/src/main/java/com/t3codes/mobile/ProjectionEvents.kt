package com.t3codes.mobile

import com.t3codes.core.Message
import com.t3codes.core.RuntimeRequest
import com.t3codes.core.T3Core
import com.t3codes.core.ThreadProjection
import com.t3codes.core.ThreadShell
import com.t3codes.core.TurnItem
import com.t3codes.core.VisibleTurnItem
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Applies one orchestration domain event to a thread projection.
 *
 * The native watcher already keeps an authoritative projection and the app
 * normally uses that. This reducer exists so the UI still moves when a state
 * JSON response is temporarily unreadable: the event payloads themselves are
 * enough to append user messages, stream assistant text and expose approvals.
 */
fun JsonElement?.domainEventType(): String =
    ((this as? JsonObject)?.get("type") as? JsonPrimitive)?.contentOrNull.orEmpty()

fun ThreadProjection.applyDomainEvent(event: JsonElement?): ThreadProjection {
    val envelope = event as? JsonObject ?: return this
    val type = envelope.string("type")
    val payload = envelope["payload"] ?: return this
    if (type.isEmpty()) return this
    return when {
        type.startsWith("thread.") -> {
            val shell = payload.decodeOrNull<ThreadShell>() ?: return this
            if (shell.id.isBlank()) this else copy(thread = shell)
        }

        type == "run.created" || type == "run.updated" -> {
            val id = (payload as? JsonObject)?.string("id").orEmpty()
            copy(runs = upsertRaw(runs, id, payload))
        }

        type == "run-attempt.created" || type == "run-attempt.updated" -> {
            val id = (payload as? JsonObject)?.string("id").orEmpty()
            copy(attempts = upsertRaw(attempts, id, payload))
        }

        type == "message.updated" -> {
            val message = payload.decodeOrNull<Message>() ?: return this
            if (message.id.isBlank()) this else copy(messages = upsertMessage(messages, message))
        }

        type == "runtime-request.updated" -> {
            val request = payload.decodeOrNull<RuntimeRequest>() ?: return this
            if (request.id.isBlank()) this else copy(runtimeRequests = upsertRequest(runtimeRequests, request))
        }

        type == "turn-item.updated" -> {
            val item = payload.decodeOrNull<TurnItem>() ?: return this
            if (item.id.isBlank()) this else applyTurnItem(item)
        }

        else -> this
    }
}

private fun ThreadProjection.applyTurnItem(item: TurnItem): ThreadProjection {
    var next = this
    val items = turnItems.toMutableList()
    val existing = items.indexOfLast { it.id == item.id }
    if (existing >= 0) items[existing] = item else items += item

    if (item.status == "rolled_back") {
        next = next.copy(
            turnItems = items,
            visibleTurnItems = next.visibleTurnItems.filterNot { it.item.id == item.id },
        )
    } else {
        val visible = visibleTurnItems.toMutableList()
        val rowIndex = visible.indexOfLast { it.item.id == item.id || it.sourceItemId == item.id }
        if (rowIndex >= 0) {
            visible[rowIndex] = visible[rowIndex].copy(item = item)
        } else {
            visible += VisibleTurnItem(
                item = item,
                visibility = "local",
                sourceItemId = item.id,
                sourceThreadId = item.threadId,
            )
        }
        visible.sortWith(compareBy({ it.item.ordinal }, { it.item.id }))
        next = next.copy(
            turnItems = items,
            visibleTurnItems = visible.mapIndexed { index, row -> row.copy(position = index) },
        )
    }
    return next
}

private fun upsertRaw(
    items: List<JsonElement>,
    id: String,
    raw: JsonElement,
): List<JsonElement> {
    if (id.isEmpty()) return items
    val index = items.indexOfLast { (it as? JsonObject)?.string("id") == id }
    if (index < 0) return items + raw
    return items.toMutableList().also { it[index] = raw }
}

private fun upsertMessage(items: List<Message>, message: Message): List<Message> {
    val index = items.indexOfLast { it.id == message.id }
    if (index < 0) return items + message
    return items.toMutableList().also { it[index] = message }
}

private fun upsertRequest(
    items: List<RuntimeRequest>,
    request: RuntimeRequest,
): List<RuntimeRequest> {
    val index = items.indexOfLast { it.id == request.id }
    if (index < 0) return items + request
    return items.toMutableList().also { it[index] = request }
}

private inline fun <reified T> JsonElement.decodeOrNull(): T? =
    runCatching { T3Core.json.decodeFromJsonElement<T>(this) }.getOrNull()

private fun JsonObject.string(key: String): String =
    (get(key) as? JsonPrimitive)?.contentOrNull.orEmpty()
