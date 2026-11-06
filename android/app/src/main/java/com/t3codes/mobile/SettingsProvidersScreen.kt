package com.t3codes.mobile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.t3codes.core.Provider
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@Composable
fun SettingsProvidersScreen(state: UiState, viewModel: MainViewModel) {
    val providers = state.config?.providers.orEmpty()

    SettingsScaffold(
        title = "Providers",
        onBack = viewModel::closeSettingsPage,
    ) {
        if (providers.isEmpty()) {
            SettingsSection {
                Text(
                    text = if (state.connected) {
                        "No providers configured."
                    } else {
                        "Connect to a server to manage providers."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        } else {
            SettingsSection {
                providers.forEachIndexed { index, provider ->
                    ProviderSettingsRow(provider)
                    if (index < providers.lastIndex) InsetDivider(start = 46.dp)
                }
            }
        }

        if (state.connected) {
            OutlinedButton(
                onClick = viewModel::refresh,
                modifier = Modifier.padding(horizontal = 4.dp),
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null)
                Text("Refresh providers")
            }
        }
    }
}

@Composable
private fun ProviderSettingsRow(provider: Provider) {
    val authStatus = (provider.auth as? JsonObject)
        ?.get("status")
        ?.let { (it as? JsonPrimitive)?.content }
    val detail = buildList {
        add(provider.status.takeIf { it.isNotBlank() } ?: if (provider.enabled) "Enabled" else "Disabled")
        authStatus?.takeIf { it.isNotBlank() }?.let { add(it) }
        provider.version?.takeIf { it.isNotBlank() }?.let { add(it) }
        add("${provider.models.size} models")
    }.joinToString(" · ")

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.SmartToy,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = provider.displayName.ifBlank { provider.driver.ifBlank { provider.instanceId } },
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
