package com.t3codes.mobile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.t3codes.core.Model
import com.t3codes.core.ModelSelection
import com.t3codes.core.ServerConfig

private const val MODEL_FAVORITES_KEY = "model_favorites"
private const val FAVORITES_FILTER = "__favorites__"

private data class ProviderGroupUi(
    val instanceId: String,
    val label: String,
    val driver: String,
    val models: List<Model>,
)

private fun modelFavoriteKey(instanceId: String, modelKey: String) = "$instanceId|$modelKey"

private fun modelMatches(choice: ModelChoice, selection: ModelSelection?): Boolean {
    if (selection == null) return false
    return selection.instanceId == choice.instanceId &&
        (selection.model == choice.model.key ||
            selection.model == choice.model.slug ||
            selection.model == choice.model.id)
}

private fun providerLabel(provider: com.t3codes.core.Provider): String =
    provider.displayName.ifBlank {
        when (provider.driver) {
            "codex" -> "Codex"
            "claudeAgent" -> "Claude"
            "pi" -> "Pi"
            "opencode" -> "OpenCode"
            else -> provider.driver.ifBlank { provider.instanceId }
        }
    }

private fun formatContextWindow(tokens: Int): String = when {
    tokens >= 1_000_000 -> "${tokens / 1_000_000}M context"
    tokens >= 1_000 -> "${tokens / 1_000}K context"
    else -> "$tokens context"
}

/** Full model catalog matching the official T3 Code thread-settings model page. */
@Composable
fun ModelPickerSheet(
    config: ServerConfig?,
    currentSelection: ModelSelection?,
    modelsRefreshing: Boolean,
    onRefresh: () -> Unit,
    onSelect: (ModelChoice) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences.of(context) }
    var favorites by remember {
        mutableStateOf(prefs.getStringSet(MODEL_FAVORITES_KEY, emptySet())?.toSet().orEmpty())
    }
    var query by remember { mutableStateOf("") }
    var providerFilter by remember { mutableStateOf<String?>(null) }
    var showLegacy by remember { mutableStateOf(false) }
    var collapsedProviders by remember { mutableStateOf(emptySet<String>()) }
    var pending by remember { mutableStateOf<ModelChoice?>(null) }
    var filterOpen by remember { mutableStateOf(false) }

    fun toggleFavorite(key: String) {
        val next = favorites.toMutableSet()
        if (!next.add(key)) next.remove(key)
        favorites = next
        prefs.edit().putStringSet(MODEL_FAVORITES_KEY, next).apply()
    }

    val providerGroups = remember(config, query, providerFilter, showLegacy, favorites, currentSelection) {
        val trimmed = query.trim()
        config?.providers.orEmpty()
            .filter { it.enabled && it.installed }
            .mapNotNull { provider ->
                val label = providerLabel(provider)
                val visible = provider.models
                    .filter { model ->
                        showLegacy || !model.isLegacy ||
                            favorites.contains(modelFavoriteKey(provider.instanceId, model.key)) ||
                            modelMatches(
                                ModelChoice(provider.instanceId, label, model),
                                currentSelection,
                            )
                    }
                    .filter { model ->
                        when (providerFilter) {
                            null -> true
                            FAVORITES_FILTER ->
                                favorites.contains(modelFavoriteKey(provider.instanceId, model.key))
                            else -> providerFilter == provider.instanceId
                        }
                    }
                    .filter { model ->
                        trimmed.isEmpty() ||
                            model.label.contains(trimmed, ignoreCase = true) ||
                            model.key.contains(trimmed, ignoreCase = true) ||
                            model.description.contains(trimmed, ignoreCase = true) ||
                            label.contains(trimmed, ignoreCase = true)
                    }
                    .sortedWith(
                        compareByDescending<Model> {
                            favorites.contains(modelFavoriteKey(provider.instanceId, it.key))
                        },
                    )
                if (visible.isEmpty()) return@mapNotNull null
                ProviderGroupUi(
                    instanceId = provider.instanceId,
                    label = label,
                    driver = provider.driver,
                    models = visible,
                )
            }
    }

    val pendingApplied = pending != null && pending?.let { choice ->
        modelMatches(choice, currentSelection)
    } == true

    fun commit() {
        val choice = pending ?: return
        if (modelMatches(choice, currentSelection)) {
            onDismiss()
            return
        }
        onSelect(choice)
        onDismiss()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 2.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Model",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (pending != null && !pendingApplied) {
                TextButton(onClick = ::commit) { Text("Save") }
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close",
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            placeholder = { Text("Find a model") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Clear search",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
            ),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box {
                Surface(
                    onClick = { filterOpen = true },
                    shape = CircleShape,
                    color = if (providerFilter != null || showLegacy) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    contentColor = if (providerFilter != null || showLegacy) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FilterList,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = when (providerFilter) {
                                null -> "All providers"
                                FAVORITES_FILTER -> "Favorites"
                                else -> providerGroups.firstOrNull { it.instanceId == providerFilter }?.label
                                    ?: "Provider"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                DropdownMenu(
                    expanded = filterOpen,
                    onDismissRequest = { filterOpen = false },
                ) {
                    FilterMenuItem(
                        label = "All providers",
                        selected = providerFilter == null,
                        onClick = {
                            providerFilter = null
                            filterOpen = false
                        },
                    )
                    FilterMenuItem(
                        label = "Favorites",
                        selected = providerFilter == FAVORITES_FILTER,
                        onClick = {
                            providerFilter = FAVORITES_FILTER
                            filterOpen = false
                        },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                    config?.providers.orEmpty()
                        .filter { it.enabled && it.installed }
                        .forEach { provider ->
                            val label = providerLabel(provider)
                            FilterMenuItem(
                                label = label,
                                selected = providerFilter == provider.instanceId,
                                onClick = {
                                    providerFilter = provider.instanceId
                                    filterOpen = false
                                },
                            )
                        }
                    val hasLegacy = config?.providers.orEmpty()
                        .any { provider -> provider.models.any { it.isLegacy } }
                    if (hasLegacy) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                        FilterMenuItem(
                            label = "Show legacy models",
                            selected = showLegacy,
                            onClick = {
                                showLegacy = !showLegacy
                                filterOpen = false
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = onRefresh,
                enabled = !modelsRefreshing,
            ) {
                if (modelsRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(17.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Refresh models",
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
        }

        if (providerGroups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = when {
                        modelsRefreshing -> "Loading models…"
                        providerFilter == FAVORITES_FILTER && query.isBlank() -> "No favorite models"
                        query.isNotBlank() || providerFilter != null -> "No matching models"
                        else -> "No available models"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            ) {
                providerGroups.forEach { group ->
                    val collapsed = group.instanceId in collapsedProviders
                    item(key = "provider:${group.instanceId}") {
                        ProviderHeader(
                            group = group,
                            collapsed = collapsed,
                            onToggle = {
                                collapsedProviders = if (collapsed) {
                                    collapsedProviders - group.instanceId
                                } else {
                                    collapsedProviders + group.instanceId
                                }
                            },
                        )
                    }
                    if (!collapsed) {
                        item(key = "models:${group.instanceId}") {
                            GroupedCard {
                                group.models.forEachIndexed { index, model ->
                                    val choice = ModelChoice(group.instanceId, group.label, model)
                                    val applied = modelMatches(choice, currentSelection)
                                    val selected = pending?.let {
                                        it.instanceId == choice.instanceId &&
                                            it.model.key == choice.model.key
                                    } ?: applied
                                    ModelRow(
                                        model = model,
                                        providerLabel = group.label,
                                        selected = selected,
                                        isFavorite = favorites.contains(
                                            modelFavoriteKey(group.instanceId, model.key),
                                        ),
                                        onToggleFavorite = {
                                            toggleFavorite(
                                                modelFavoriteKey(group.instanceId, model.key),
                                            )
                                        },
                                        onSelect = {
                                            pending = if (applied) null else choice
                                        },
                                    )
                                    if (index < group.models.lastIndex) {
                                        InsetDivider(start = 16.dp)
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderHeader(
    group: ProviderGroupUi,
    collapsed: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(start = 4.dp, end = 6.dp, top = 12.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.SmartToy,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = group.label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = group.models.size.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            imageVector = if (collapsed) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess,
            contentDescription = if (collapsed) "Expand" else "Collapse",
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        )
    }
}

@Composable
private fun ModelRow(
    model: Model,
    providerLabel: String,
    selected: Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onSelect: () -> Unit,
) {
    val subtitle = model.description.lineSequence().firstOrNull()?.trim().orEmpty()
        .ifBlank {
            model.subProvider.ifBlank {
                model.contextWindow?.let(::formatContextWindow).orEmpty()
            }
        }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(start = 16.dp, end = 4.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 2.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = model.label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (model.isDefault) {
                    Spacer(Modifier.width(6.dp))
                    ModelBadge("Default", strong = true)
                }
                if (model.isLegacy) {
                    Spacer(Modifier.width(6.dp))
                    ModelBadge("Legacy", strong = false)
                }
            }
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else if (providerLabel.isNotBlank() && model.subProvider.isBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = providerLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                )
            }
        }
        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                contentDescription = if (isFavorite) "Remove favorite" else "Add favorite",
                modifier = Modifier.size(19.dp),
                tint = if (isFavorite) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                },
            )
        }
        Box(
            modifier = Modifier.size(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Selected",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun ModelBadge(text: String, strong: Boolean) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (strong) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
        )
    }
}

@Composable
private fun FilterMenuItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        trailingIcon = {
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        },
        onClick = onClick,
    )
}
