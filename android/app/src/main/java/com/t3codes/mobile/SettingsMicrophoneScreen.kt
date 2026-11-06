package com.t3codes.mobile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

private val MIC_KIND_DEFAULT_ORDER = listOf("wired", "bluetooth", "builtIn")

private fun micKindLabel(kind: String): String = when (kind) {
    "wired" -> "Wired / USB"
    "bluetooth" -> "Bluetooth"
    "builtIn" -> "Built-in mic"
    else -> kind
}

private fun micKindIcon(kind: String) = when (kind) {
    "wired" -> Icons.Rounded.Headset
    "bluetooth" -> Icons.Rounded.Bluetooth
    else -> Icons.Rounded.PhoneAndroid
}

private fun storedMicOrder(state: UiState): List<String> =
    state.microphoneOrder.ifEmpty { MIC_KIND_DEFAULT_ORDER }

@Composable
fun SettingsMicrophoneScreen(state: UiState, viewModel: MainViewModel) {
    val context = LocalContext.current
    var permissionGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val requestPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> permissionGranted = granted }

    val order = storedMicOrder(state)

    SettingsScaffold(
        title = "Microphone",
        onBack = viewModel::closeSettingsPage,
    ) {
        SettingsSection(title = "Microphone access") {
            Column {
                SettingsRow(
                    icon = Icons.Rounded.Mic,
                    title = "Microphone access",
                    value = if (permissionGranted) "Allowed" else "Not allowed",
                    onClick = if (permissionGranted) null else {
                        {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                requestPermission.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    trailing = if (permissionGranted) {
                        { Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    } else {
                        null
                    },
                )
            }
        }
        SettingsSection(title = "Preferred order") {
            Column {
                order.forEachIndexed { index, kind ->
                    if (index > 0) InsetDivider(start = 52.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = micKindIcon(kind),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = micKindLabel(kind),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = { viewModel.moveMicrophone(kind, -1) },
                            enabled = index > 0,
                        ) { Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move up") }
                        IconButton(
                            onClick = { viewModel.moveMicrophone(kind, 1) },
                            enabled = index < order.lastIndex,
                        ) { Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move down") }
                    }
                }
            }
        }
        SettingsNote("When several microphones are available, voice input uses the first one in this order.")
    }
}
