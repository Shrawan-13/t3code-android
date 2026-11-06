package com.t3codes.mobile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.t3codes.core.ScheduledTask
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun SettingsScheduledTasksScreen(state: UiState, viewModel: MainViewModel) {
    LaunchedEffect(state.connected) {
        if (state.connected) viewModel.refreshScheduledTasks()
    }

    SettingsScaffold(
        title = "Scheduled tasks",
        onBack = viewModel::closeSettingsPage,
        actions = {
            IconButton(onClick = viewModel::refreshScheduledTasks) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
            }
        },
    ) {
        if (!state.connected) {
            SettingsSection {
                Text(
                    text = "Connect to a server to manage scheduled tasks.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        } else if (state.scheduledTasksLoading && state.scheduledTasks.isEmpty()) {
            SettingsSection {
                Text(
                    text = "Loading tasks…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        } else if (state.scheduledTasks.isEmpty()) {
            SettingsSection {
                Text(
                    text = "No scheduled tasks.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        } else {
            state.scheduledTasks.forEach { task ->
                SettingsSection {
                    ScheduledTaskRow(task, viewModel)
                }
            }
        }
    }
}

@Composable
private fun ScheduledTaskRow(task: ScheduledTask, viewModel: MainViewModel) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title.ifBlank { "Untitled task" },
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = scheduleLabel(task) + taskStatusSuffix(task),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Switch(
                checked = task.enabled,
                onCheckedChange = { viewModel.setScheduledTaskEnabled(task.id, it) },
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { viewModel.runScheduledTaskNow(task.id) }) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Text("Run now")
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { viewModel.deleteScheduledTask(task.id) }) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun scheduleLabel(task: ScheduledTask): String {
    val schedule = task.schedule as? JsonObject ?: return "No schedule"
    val type = (schedule["type"] as? JsonPrimitive)?.content
    return when (type) {
        "interval" -> {
            val everyMs = schedule["everyMs"]?.jsonPrimitive?.intOrNull
            when {
                everyMs == null -> "Repeating"
                everyMs >= 3_600_000 -> "Every ${everyMs / 3_600_000}h"
                else -> "Every ${(everyMs / 60_000).coerceAtLeast(1)}m"
            }
        }
        "fixed_time" -> {
            val time = (schedule["timeOfDay"] as? JsonPrimitive)?.content ?: ""
            val weekdays = schedule["weekdays"]?.toString()
            if (weekdays.isNullOrBlank()) "Daily at $time" else "At $time"
        }
        "once" -> "Once"
        "webhook" -> "Webhook"
        else -> type?.replaceFirstChar { it.uppercase() } ?: "No schedule"
    }
}

private fun taskStatusSuffix(task: ScheduledTask): String = buildList {
    task.lastRunStatus.takeIf { it.isNotBlank() }?.let { add("last: $it") }
    if (task.runCount > 0) add("${task.runCount} runs")
}.joinToString(" · ").let { if (it.isBlank()) "" else " · $it" }
