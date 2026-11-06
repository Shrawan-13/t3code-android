package com.t3codes.mobile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@Composable
fun SettingsDiagnosticsScreen(state: UiState, viewModel: MainViewModel) {
    val context = LocalContext.current
    val latestCrash = remember { CrashReporter.latest(context) }
    val environment = state.config?.environment

    SettingsScaffold(
        title = "Diagnostics",
        onBack = viewModel::closeSettingsPage,
    ) {
        SettingsSection(title = "Connection") {
            Column {
                SettingsValueRow(title = "Server", value = state.baseUrl.ifBlank { "Not connected" })
                InsetDivider(start = 14.dp)
                SettingsValueRow(
                    title = "Protocol",
                    value = environment?.orchestrationProtocolVersion?.toString() ?: "unknown",
                )
                InsetDivider(start = 14.dp)
                SettingsValueRow(
                    title = "Platform",
                    value = environment?.platform?.let { "${it.os}/${it.arch}" } ?: "unknown",
                    monospace = false,
                )
            }
        }

        SettingsSection(title = "Self-test") {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Probe shell, threads, and usage",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(
                        onClick = viewModel::runSelfTest,
                        enabled = !state.selfTestBusy,
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        if (state.selfTestBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(15.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("Run")
                    }
                }
                if (state.selfTestLog.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            state.selfTestLog.forEach { line ->
                                Text(
                                    text = line,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }

        SettingsSection(title = "Crash reports") {
            Column {
                SettingsRow(
                    icon = Icons.Rounded.BugReport,
                    title = if (latestCrash != null) "Open latest crash log" else "No crash logs",
                    enabled = latestCrash != null,
                    onClick = latestCrash?.let { file ->
                        {
                            CrashReporter.launchCrashScreen(
                                context = context,
                                file = file,
                                replaceTask = false,
                            )
                        }
                    },
                )
                if (latestCrash != null) {
                    Text(
                        text = latestCrash.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}
