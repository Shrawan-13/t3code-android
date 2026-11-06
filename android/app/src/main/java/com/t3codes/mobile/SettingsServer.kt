package com.t3codes.mobile

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Scope helpers for server-scoped settings pages. Null scope = All projects (machine-wide). */

fun scopeKey(projectId: String?): String? = projectId?.takeIf { it.isNotBlank() }

fun scopeBool(state: UiState, key: String, default: Boolean): Boolean =
    ServerSettingsView(state.serverSettings)
        .effectiveBool(scopeKey(state.serverScopeProjectId), key, default)

fun scopeString(state: UiState, key: String, default: String? = null): String? =
    ServerSettingsView(state.serverSettings)
        .effectiveString(scopeKey(state.serverScopeProjectId), key, default)

fun scopeStringNullable(state: UiState, key: String): Pair<Boolean, String?> {
    // (explicitOverride, effective value or null)
    val projectId = scopeKey(state.serverScopeProjectId)
    if (projectId == null) {
        return false to ServerSettingsView(state.serverSettings).string(key)
    }
    val (has, value, explicitNull) = ServerSettingsView(state.serverSettings).overrideOrNullStored(projectId, key)
    if (!has) return false to ServerSettingsView(state.serverSettings).string(key)
    if (explicitNull) return true to null
    return true to (value as? kotlinx.serialization.json.JsonPrimitive)?.content
}

fun scopeIntNullable(state: UiState, key: String, default: Int? = null): Int? =
    ServerSettingsView(state.serverSettings)
        .effectiveIntNullable(scopeKey(state.serverScopeProjectId), key, default)

fun scopeHasOverride(state: UiState, key: String): Boolean {
    val projectId = scopeKey(state.serverScopeProjectId) ?: return false
    return ServerSettingsView(state.serverSettings).hasOverride(projectId, key)
}

fun projectLabel(project: com.t3codes.core.Project?): String =
    project?.title?.ifBlank { null }
        ?: project?.workspaceRoot?.substringAfterLast('/')
        ?: "Project"

/** Project chips shown at the top of server-scoped pages. */
@Composable
fun ServerScopeChips(state: UiState, viewModel: MainViewModel) {
    if (state.projects.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = state.serverScopeProjectId == null,
            onClick = { viewModel.setServerScope(null) },
            label = { Text("All projects") },
        )
        state.projects.forEach { project ->
            FilterChip(
                selected = state.serverScopeProjectId == project.id,
                onClick = { viewModel.setServerScope(project.id) },
                label = { Text(projectLabel(project)) },
            )
        }
    }
}

/** Small "Managed by <org>" caption shown under a locked control. Null when unlocked. */
@Composable
fun ManagedNote(state: UiState, path: String) {
    val org = ManagedPolicy.notice(state.config, path) ?: return
    Text(
        text = org,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

/** True when every path is locked by an administrator's policy. */
fun anyLocked(state: UiState, vararg paths: String): Boolean =
    paths.any { ManagedPolicy.isLocked(state.config, it) }

/** Drop this project's overrides for the page's keys. Shown only when something is overridden. */
@Composable
fun ClearProjectOverridesRow(
    state: UiState,
    viewModel: MainViewModel,
    keys: List<String>,
) {
    val projectId = scopeKey(state.serverScopeProjectId) ?: return
    val view = ServerSettingsView(state.serverSettings)
    val overridden = keys.filter { view.hasOverride(projectId, it) }
    if (overridden.isEmpty()) return
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(
            onClick = { viewModel.clearProjectOverrides(projectId, overridden) },
            enabled = !state.serverSettingsWriting,
        ) {
            Text("Clear project overrides", fontWeight = FontWeight.SemiBold)
        }
    }
}
