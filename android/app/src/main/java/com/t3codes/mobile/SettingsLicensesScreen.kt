package com.t3codes.mobile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private data class LicenseEntry(val name: String, val license: String)

private val OPEN_SOURCE_LICENSES = listOf(
    LicenseEntry("Kotlin", "Apache License 2.0"),
    LicenseEntry("Kotlin Coroutines", "Apache License 2.0"),
    LicenseEntry("kotlinx.serialization", "Apache License 2.0"),
    LicenseEntry("Jetpack Compose", "Apache License 2.0"),
    LicenseEntry("AndroidX", "Apache License 2.0"),
    LicenseEntry("Material Icons", "Apache License 2.0"),
    LicenseEntry("OkHttp", "Apache License 2.0"),
    LicenseEntry("T3 Code", "Apache License 2.0"),
)

@Composable
fun SettingsLicensesScreen(state: UiState, viewModel: MainViewModel) {
    SettingsScaffold(
        title = "Open source licenses",
        onBack = viewModel::closeSettingsPage,
    ) {
        SettingsSection {
            Column {
                OPEN_SOURCE_LICENSES.forEachIndexed { index, entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = entry.name, style = MaterialTheme.typography.labelLarge)
                            Text(
                                text = entry.license,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (index < OPEN_SOURCE_LICENSES.lastIndex) InsetDivider(start = 46.dp)
                }
            }
        }
        Text(
            text = "Full license texts ship with each dependency.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}
