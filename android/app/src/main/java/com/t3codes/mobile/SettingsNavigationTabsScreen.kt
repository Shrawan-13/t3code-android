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
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CallMerge
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsNavigationTabsScreen(state: UiState, viewModel: MainViewModel) {
    val labels = mapOf("threads" to "Threads", "pullRequests" to "Pull requests", "usage" to "Usage")
    val icons = mapOf(
        "threads" to Icons.Rounded.Forum,
        "pullRequests" to Icons.Rounded.CallMerge,
        "usage" to Icons.Rounded.BarChart,
    )
    val hidden = state.navHiddenTabs

    SettingsScaffold(
        title = "Navigation tabs",
        onBack = viewModel::closeSettingsPage,
    ) {
        SettingsNote("Reorder and hide tabs. Hidden sections stay reachable from Settings. Settings always stays last.")
        SettingsSection(title = "Tab order") {
            Column {
                state.navTabOrder.forEachIndexed { index, tab ->
                    if (index > 0) InsetDivider(start = 58.dp)
                    val isHidden = tab in hidden
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = icons.getValue(tab),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = labels.getValue(tab),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isHidden) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = if (isHidden) "Hidden" else "Tab ${state.navTabOrder.filterNot { it in hidden }.indexOf(tab) + 1}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { viewModel.toggleNavTabHidden(tab) }) {
                            Icon(
                                imageVector = if (isHidden) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = if (isHidden) "Show ${labels[tab]}" else "Hide ${labels[tab]}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(
                            onClick = { viewModel.moveNavTab(tab, -1) },
                            enabled = index > 0,
                        ) {
                            Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move up")
                        }
                        IconButton(
                            onClick = { viewModel.moveNavTab(tab, 1) },
                            enabled = index < state.navTabOrder.lastIndex,
                        ) {
                            Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move down")
                        }
                    }
                }
            }
        }
        Button(
            onClick = viewModel::resetNavTabs,
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Text("Reset tabs")
        }
    }
}
