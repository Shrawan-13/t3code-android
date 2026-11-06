package com.t3codes.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun SettingsClientStorageScreen(state: UiState, viewModel: MainViewModel) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    var confirmClear by remember { mutableStateOf(false) }
    val bytes = remember(refresh) { cacheBytes(context) }

    SettingsScaffold(
        title = "Client storage",
        onBack = viewModel::closeSettingsPage,
    ) {
        SettingsSection(title = "Cache") {
            Column {
                SettingsRow(
                    icon = Icons.Rounded.Storage,
                    title = "Cached threads and sessions",
                    value = formatCacheBytes(bytes),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = { refresh++ },
                        modifier = Modifier.weight(1f),
                    ) { Text("Recalculate") }
                    OutlinedButton(
                        onClick = { confirmClear = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        androidx.compose.material3.Icon(
                            Icons.Rounded.DeleteSweep,
                            contentDescription = null,
                        )
                        Text("Clear")
                    }
                }
            }
        }
        Text(
            text = "Clearing removes locally cached thread data. Server data is untouched.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear client cache?") },
            text = { Text("Cached threads reload from the server the next time they are opened.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        viewModel.clearClientCache()
                        refresh++
                    },
                ) { Text("Clear", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancel") }
            },
        )
    }
}

private fun cacheBytes(context: android.content.Context): Long {
    val base = context.getDatabasePath("t3-client-cache.db")
    return listOf(base, File("${base.path}-wal"), File("${base.path}-shm"))
        .filter { it.exists() }
        .sumOf { it.length() }
}

private fun formatCacheBytes(bytes: Long): String {
    if (bytes <= 0) return "Empty"
    val units = listOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var index = 0
    while (value >= 1024 && index < units.lastIndex) {
        value /= 1024
        index++
    }
    return if (index == 0) "$bytes B" else String.format("%.1f %s", value, units[index])
}
