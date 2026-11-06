package com.t3codes.mobile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun SettingsNotificationsScreen(state: UiState, viewModel: MainViewModel) {
    val context = LocalContext.current
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

    SettingsScaffold(
        title = "Notifications",
        onBack = viewModel::closeSettingsPage,
    ) {
        SettingsSection(title = "Background sync") {
            Column {
                SettingsSwitchRow(
                    icon = Icons.Rounded.NotificationsActive,
                    title = "Background sync",
                    checked = state.backgroundSyncEnabled,
                    onCheckedChange = toggleBackgroundSync,
                )
            }
        }
        SettingsNote(
            if (state.backgroundSyncEnabled) {
                "Watching your chats and sending completion or input alerts, even with the app closed. A persistent notification keeps sync alive; use Stop on it to end."
            } else {
                "Keep chats updated and get notified when they finish or need input. A persistent notification keeps sync alive while it runs."
            },
        )
    }
}
