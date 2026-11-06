package com.t3codes.mobile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.t3codes.core.ThreadShell

@Composable
fun SettingsArchiveScreen(state: UiState, viewModel: MainViewModel) {
    SettingsScaffold(
        title = "Archive",
        onBack = viewModel::closeSettingsPage,
    ) {
        if (state.archivedThreads.isEmpty()) {
            SettingsSection {
                Text(
                    text = "No archived threads.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        } else {
            state.archivedThreads.forEach { thread ->
                SettingsSection {
                    ArchivedThreadRow(thread, state, viewModel)
                }
            }
        }
    }
}

@Composable
private fun ArchivedThreadRow(thread: ThreadShell, state: UiState, viewModel: MainViewModel) {
    val project = state.projects.firstOrNull { it.id == thread.projectId }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Archive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = thread.title.ifBlank { "Untitled thread" },
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = project?.title?.ifBlank { null } ?: "Archived",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { viewModel.threadAction(thread.id, "unarchive") }) {
                Icon(Icons.Rounded.Unarchive, contentDescription = null)
                Text("Restore")
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { viewModel.threadAction(thread.id, "delete") }) {
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
