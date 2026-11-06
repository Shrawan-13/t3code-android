package com.t3codes.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsEnvironmentsScreen(state: UiState, viewModel: MainViewModel) {
    var confirmDisconnect by remember { mutableStateOf(false) }
    val environment = state.config?.environment

    SettingsScaffold(
        title = "Environments",
        onBack = viewModel::closeSettingsPage,
    ) {
        SettingsSection(title = "Connection") {
            SettingsValueRow(
                title = "Server",
                value = state.baseUrl.ifBlank { "Not connected" },
            )
            InsetDivider(start = 14.dp)
            SettingsValueRow(
                title = "Environment",
                value = environment?.environmentId?.ifBlank { "unknown" } ?: "unknown",
            )
            InsetDivider(start = 14.dp)
            SettingsValueRow(
                title = "Version",
                value = environment?.serverVersion?.ifBlank { "?" } ?: "?",
            )
            InsetDivider(start = 14.dp)
            SettingsValueRow(
                title = "Platform",
                value = environment?.platform?.let { "${it.os}/${it.arch}" } ?: "unknown",
                monospace = false,
            )
        }

        SettingsSection(title = "Actions") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = viewModel::refresh,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                    Text("Refresh")
                }
                OutlinedButton(
                    onClick = { confirmDisconnect = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Icon(Icons.Rounded.Logout, contentDescription = null)
                    Text("Disconnect")
                }
            }
        }

    }

    if (confirmDisconnect) {
        AlertDialog(
            onDismissRequest = { confirmDisconnect = false },
            title = { Text("Disconnect?") },
            text = { Text("Your saved server URL and token stay on this device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDisconnect = false
                        viewModel.disconnect()
                    },
                ) { Text("Disconnect", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDisconnect = false }) { Text("Cancel") }
            },
        )
    }
}
