package com.t3codes.mobile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.t3codes.core.Provider

@Composable
fun SettingsScreen(state: UiState, viewModel: MainViewModel) {
    val config = state.config ?: return
    val environment = config.environment
    var confirmDisconnect by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val latestCrash = remember { CrashReporter.latest(context) }
    var enableAfterPermission by remember { mutableStateOf(false) }
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted && enableAfterPermission) {
            viewModel.setBackgroundSync(true)
        } else if (!granted) {
            viewModel.showMessage("Allow notifications to use background sync.")
        }
        enableAfterPermission = false
    }
    val toggleBackgroundSync: (Boolean) -> Unit = { enabled ->
        if (!enabled) {
            viewModel.setBackgroundSync(false)
        } else if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            enableAfterPermission = true
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.setBackgroundSync(true)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ScreenHeader(
                title = "Settings",
                subtitle = environment.label.ifBlank { "Connected environment" },
                leading = {
                    BackButton { viewModel.selectTab(AppTab.Threads) }
                },
            )
        }

        item {
            GroupedCard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusDot(StatusTone.Active, pulsing = true, size = 9.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = environment.label.ifBlank { "Connected environment" },
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = state.baseUrl,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Pill(
                        text = "Online",
                        container = MaterialTheme.colorScheme.tertiaryContainer,
                        content = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
        }

        item { SectionLabel("Workspace") }
        item {
            GroupedCard {
                SettingsRow(
                    icon = Icons.Rounded.FolderOpen,
                    title = "Projects",
                    subtitle = "${state.projects.size} project${if (state.projects.size == 1) "" else "s"}",
                    onClick = { viewModel.selectTab(AppTab.Projects) },
                )
                InsetDivider(start = 58.dp)
                SettingsRow(
                    icon = Icons.Rounded.BarChart,
                    title = "Usage",
                    subtitle = "Tokens, cost, and model activity",
                    onClick = { viewModel.selectTab(AppTab.Usage) },
                )
            }
        }

        item { SectionLabel("Connection") }
        item {
            GroupedCard {
                SettingsRow(
                    icon = Icons.Rounded.Dns,
                    title = "Server",
                    subtitle = state.baseUrl,
                    monospace = true,
                )
                InsetDivider(start = 58.dp)
                SettingsRow(
                    icon = Icons.Rounded.Smartphone,
                    title = "Environment",
                    subtitle = environment.environmentId.ifBlank { "unknown" },
                    monospace = true,
                )
                InsetDivider(start = 58.dp)
                SettingsRow(
                    icon = Icons.Rounded.Info,
                    title = "Version",
                    subtitle = "T3 ${environment.serverVersion.ifBlank { "?" }} · protocol " +
                        environment.orchestrationProtocolVersion +
                        " · ${environment.platform.os}/${environment.platform.arch}",
                )
                InsetDivider(start = 58.dp)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = viewModel::refresh,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Refresh")
                    }
                    OutlinedButton(
                        onClick = { confirmDisconnect = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    ) {
                        Icon(Icons.Rounded.Logout, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Disconnect")
                    }
                }
            }
        }

        item { SectionLabel("Notifications") }
        item {
            GroupedCard {
                SettingsRow(
                    icon = Icons.Rounded.Notifications,
                    title = "Background sync",
                    subtitle = if (state.backgroundSyncEnabled) {
                        "Watching active chats and sending completion or input alerts."
                    } else {
                        "Keep active chats updated and notify you when they finish or need input."
                    },
                    trailing = {
                        Switch(
                            checked = state.backgroundSyncEnabled,
                            onCheckedChange = toggleBackgroundSync,
                        )
                    },
                )
            }
        }

        item { SectionLabel("Providers · ${config.providers.size}") }
        item {
            GroupedCard {
                if (config.providers.isEmpty()) {
                    Text(
                        text = "No providers configured on this server.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                } else {
                    config.providers.forEachIndexed { index, provider ->
                        ProviderRow(provider)
                        if (index < config.providers.lastIndex) InsetDivider(start = 16.dp)
                    }
                }
            }
        }

        item { SectionLabel("Appearance") }
        item {
            GroupedCard {
                SettingsRow(
                    icon = Icons.Rounded.Palette,
                    title = "Theme",
                    subtitle = "Follow the system or pin light / dark.",
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ThemePref.entries.forEach { pref ->
                        FilterChip(
                            selected = state.themePref == pref,
                            onClick = { viewModel.setThemePref(pref) },
                            label = { Text(pref.name) },
                            shape = CircleShape,
                        )
                    }
                }
            }
        }

        item { SectionLabel("Diagnostics") }
        item {
            GroupedCard {
                SettingsRow(
                    icon = Icons.Rounded.BugReport,
                    title = "Crash reports",
                    subtitle = latestCrash?.let {
                        "Open the latest log and share it with the developer"
                    } ?: "No crash logs on this device",
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
                InsetDivider(start = 58.dp)
                SettingsRow(
                    icon = Icons.Rounded.CheckCircle,
                    title = "Core self-test",
                    subtitle = "Probe shell, thread projection, and usage endpoints.",
                    trailing = {
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
                    },
                )
                if (state.selfTestLog.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                        shape = RoundedCornerShape(16.dp),
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

        item { SectionLabel("About") }
        item {
            GroupedCard {
                SettingsRow(
                    icon = Icons.Rounded.Code,
                    title = "Go core",
                    subtitle = "shrawan-13/t3code-core · built for arm64-v8a",
                )
                InsetDivider(start = 58.dp)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "T3 Code for Android ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = {
                            uriHandler.openUri("https://github.com/Shrawan-13/t3code-core")
                        },
                    ) { Text("Core repo") }
                }
            }
        }
    }

    if (confirmDisconnect) {
        AlertDialog(
            onDismissRequest = { confirmDisconnect = false },
            title = { Text("Disconnect?") },
            text = { Text("Your saved server URL and token will stay on this device.") },
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

@Composable
private fun ProviderRow(provider: Provider) {
    val tone = when {
        provider.enabled && provider.installed -> MaterialTheme.colorScheme.primary
        !provider.enabled -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.error
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusDot(tone = if (provider.installed) StatusTone.Active else StatusTone.Neutral, size = 8.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = provider.displayName.ifBlank { provider.driver.ifBlank { provider.instanceId } },
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(
                    provider.status.takeIf { it.isNotBlank() },
                    provider.version?.takeIf { it.isNotBlank() },
                    "${provider.models.size} models",
                ).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = if (provider.enabled) "on" else "off",
            style = MaterialTheme.typography.labelSmall,
            color = tone,
        )
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    monospace: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(10.dp))
            trailing()
        }
    }
}
