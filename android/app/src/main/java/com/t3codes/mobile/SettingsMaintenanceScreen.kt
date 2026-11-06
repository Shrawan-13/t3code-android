package com.t3codes.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull

@Composable
fun SettingsMaintenanceScreen(state: UiState, viewModel: MainViewModel) {
    val currentVersion = state.config?.environment?.serverVersion.orEmpty()

    SettingsScaffold(
        title = "Maintenance",
        onBack = viewModel::closeSettingsPage,
    ) {
        if (!state.connected) {
            SettingsSection {
                Text(
                    text = "Connect to a server to run maintenance.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        } else {
        SettingsSection(title = "Server update") {
            Column {
                SettingsRow(
                    icon = Icons.Rounded.CloudDownload,
                    title = "Current version",
                    value = currentVersion.ifBlank { "unknown" },
                )
                val update = state.serverUpdate
                if (update != null) {
                    InsetDivider(start = 46.dp)
                    SettingsRow(
                        icon = Icons.Rounded.CloudDownload,
                        title = "Latest release",
                        value = update.version.ifBlank { "unknown" },
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = viewModel::checkServerUpdate,
                        enabled = !state.maintenanceBusy,
                        modifier = Modifier.weight(1f),
                    ) {
                        if (state.maintenanceBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("Check for updates")
                        }
                    }
                    if (update != null && update.version.isNotBlank() && update.version != currentVersion) {
                        OutlinedButton(
                            onClick = { viewModel.installServerUpdate(update.version, update.databaseMigration) },
                            enabled = !state.maintenanceBusy,
                            modifier = Modifier.weight(1f),
                        ) { Text("Install ${update.version}") }
                    }
                }
                state.maintenanceMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    )
                }
            }
        }

        SettingsSection(title = "Storage cleanup") {
            Column {
                SettingsRow(
                    icon = Icons.Rounded.Storage,
                    title = "Server storage",
                    value = storageSummary(state.storageReport),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = viewModel::loadStorageReport,
                        enabled = !state.maintenanceBusy,
                        modifier = Modifier.weight(1f),
                    ) { Text("Scan") }
                    OutlinedButton(
                        onClick = { viewModel.runStorageCleanup(false) },
                        enabled = !state.maintenanceBusy,
                        modifier = Modifier.weight(1f),
                    ) {
                        androidx.compose.material3.Icon(
                            Icons.Rounded.CleaningServices,
                            contentDescription = null,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Clean up")
                    }
                }
                val entries = (state.storageReport?.get("entries") as? kotlinx.serialization.json.JsonArray)?.size ?: 0
                if (entries > 0) {
                    Text(
                        text = "$entries items evaluated",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    )
                }
            }
        }
        }
    }
}

private fun storageSummary(report: JsonObject?): String {
    if (report == null) return "Not scanned"
    val counts = report["counts"] as? JsonObject
    val removed = (counts?.get("removed") as? JsonPrimitive)?.intOrNull ?: 0
    val kept = (counts?.get("kept") as? JsonPrimitive)?.intOrNull ?: 0
    val failed = (counts?.get("failed") as? JsonPrimitive)?.intOrNull ?: 0
    val bytes = (report["bytesFreed"] as? JsonPrimitive)?.longOrNull ?: 0L
    return "$removed removed · $kept kept · $failed failed · ${formatBytes(bytes)}"
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = listOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var index = 0
    while (value >= 1024 && index < units.lastIndex) {
        value /= 1024
        index++
    }
    return if (index == 0) "${bytes} B" else String.format("%.1f %s", value, units[index])
}
