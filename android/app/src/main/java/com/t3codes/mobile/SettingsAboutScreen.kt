package com.t3codes.mobile

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp

@Composable
fun SettingsAboutScreen(state: UiState, viewModel: MainViewModel) {
    val uriHandler = LocalUriHandler.current
    val environment = state.config?.environment

    SettingsScaffold(
        title = "About",
        onBack = viewModel::closeSettingsPage,
    ) {
        SettingsSection(title = "App") {
            Column {
                SettingsValueRow(
                    title = "T3 Code for Android",
                    value = BuildConfig.VERSION_NAME,
                    monospace = false,
                )
                InsetDivider(start = 14.dp)
                SettingsValueRow(
                    title = "Server",
                    value = environment?.serverVersion?.ifBlank { "Not connected" } ?: "Not connected",
                )
            }
        }

        SettingsSection(title = "Storage & diagnostics") {
            Column {
                SettingsRow(
                    icon = Icons.Rounded.Storage,
                    title = "Client storage",
                    onClick = { viewModel.openSettingsPage(SettingsPages.ClientStorage) },
                )
                InsetDivider(start = 46.dp)
                SettingsRow(
                    icon = Icons.Rounded.BugReport,
                    title = "Diagnostics",
                    onClick = { viewModel.openSettingsPage(SettingsPages.Diagnostics) },
                )
                InsetDivider(start = 46.dp)
                SettingsRow(
                    icon = Icons.Rounded.Description,
                    title = "Open source licenses",
                    onClick = { viewModel.openSettingsPage(SettingsPages.Licenses) },
                )
            }
        }

        SettingsSection(title = "Source") {
            Column {
                SettingsRow(
                    icon = Icons.Rounded.Code,
                    title = "Android client",
                    onClick = { uriHandler.openUri("https://github.com/Shrawan-13/t3code-android") },
                )
                InsetDivider(start = 46.dp)
                SettingsRow(
                    icon = Icons.Rounded.Code,
                    title = "Go core",
                    onClick = { uriHandler.openUri("https://github.com/Shrawan-13/t3code-core") },
                )
            }
        }
    }
}
