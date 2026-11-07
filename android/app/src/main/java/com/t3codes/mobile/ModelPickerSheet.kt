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
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.t3codes.core.Model
import com.t3codes.core.ModelSelection
import com.t3codes.core.ServerConfig

private const val MODEL_FAVORITES_KEY = "model_favorites"
private const val MODEL_HIDDEN_KEY = "model_hidden"
private const val MODEL_SHOW_LEGACY_KEY = "model_show_legacy"
private const val FAVORITES_FILTER = "__favorites__"

internal data class ThreadRuntimeOption(
    val value: String,
    val label: String,
    val description: String,
)

internal val threadRuntimeOptions = listOf(
    ThreadRuntimeOption(
        value = "approval-required",
        label = "Ask first",
        description = "Approve commands and file changes",
    ),
    ThreadRuntimeOption(
        value = "auto-accept-edits",
        label = "Auto-accept edits",
        description = "Approve file edits, ask before commands",
    ),
    ThreadRuntimeOption(
        value = "auto",
        label = "Auto",
        description = "Ask only when T3 thinks it is necessary",
    ),
    ThreadRuntimeOption(
        value = "full-access",
        label = "Full access",
        description = "Run commands and edit files without asking",
    ),
)

internal fun runtimeModeLabel(value: String?): String =
    threadRuntimeOptions.firstOrNull { it.value == value }?.label ?: "Full access"

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

/**
 * Full thread-settings page matching the official T3 Code mobile model picker:
 * searchable model catalog with provider sections, radio selection, per-model
 * favorites, runtime permissions, hidden-model management, and a legacy toggle.
 */
@Composable
fun ModelPickerSheet(
    config: ServerConfig?,
    currentSelection: ModelSelection?,
    modelsRefreshing: Boolean,
    onRefresh: () -> Unit,
    onSelect: (ModelChoice) -> Unit,
    onDismiss: () -> Unit,
    runtimeMode: String? = null,
    onSelectRuntime: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences.of(context) }
    var favorites by remember {
        mutableStateOf(prefs.getStringSet(MODEL_FAVORITES_KEY, emptySet())?.toSet().orEmpty())
    }
    var hidden by remember {
        mutableStateOf(prefs.getStringSet(MODEL_HIDDEN_KEY, emptySet())?.toSet().orEmpty())
    }
    var showLegacy by remember {
        mutableStateOf(prefs.getBoolean(MODEL_SHOW_LEGACY_KEY, false))
    }
    var manageHidden by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var providerFilter by remember { mutableStateOf<String?>(null) }
    var collapsedProviders by remember { mutableStateOf(emptySet<String>()) }
    var filterOpen by remember { mutableStateOf(false) }
    var runtimeOpen by remember { mutableStateOf(false) }

    fun toggleFavorite(key: String) {
        val next = favorites.toMutableSet()
        if (!next.add(key)) next.remove(key)
        favorites = next
        prefs.edit().putStringSet(MODEL_FAVORITES_KEY, next).apply()
    }

    fun toggleHidden(key: String) {
        val next = hidden.toMutableSet()
        if (!next.add(key)) next.remove(key)
        hidden = next
        prefs.edit().putStringSet(MODEL_HIDDEN_KEY, next).apply()
    }

    val providerGroups = remember(
        config,
        query,
        providerFilter,
        showLegacy,
        favorites,
        hidden,
        manageHidden,
        currentSelection,
    ) {
        val trimmed = query.trim()
        config?.providers.orEmpty()
            .filter { it.enabled && it.installed }
            .mapNotNull { provider ->
                val label = providerLabel(provider)
                val visible = provider.models
                    .filter { model ->
                        val favorite = favorites.contains(
                            modelFavoriteKey(provider.instanceId, model.key),
                        )
                        val selected = modelMatches(
                            ModelChoice(provider.instanceId, label, model),
                            currentSelection,
                        )
                        val isHidden = hidden.contains(
                            modelFavoriteKey(provider.instanceId, model.key),
                        )
                        val hiddenAllowed = manageHidden || !isHidden
                        val legacyAllowed = showLegacy || !model.isLegacy || favorite || selected
                        hiddenAllowed && legacyAllowed
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
                        }.thenBy { it.label.lowercase() },
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

    val hasLegacy = config?.providers.orEmpty()
        .any { provider -> provider.models.any { it.isLegacy } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close",
                    modifier = Modifier.size(21.dp),
                )
            }
            Text(
                text = "Thread settings",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
            )
            Box {
                IconButton(onClick = { filterOpen = true }) {
                    Icon(
                        imageVector = Icons.Rounded.FilterList,
                        contentDescription = "Filter models",
                        modifier = Modifier.size(21.dp),
                        tint = if (providerFilter != null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
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
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                    DropdownMenuItem(
                        text = { Text("Refresh models") },
                        leadingIcon = {
                            if (modelsRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        },
                        enabled = !modelsRefreshing,
                        onClick = {
                            filterOpen = false
                            onRefresh()
                        },
                    )
                }
            }
        }

        TextField(
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
                    modifier = Modifier.size(21.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
            shape = RoundedCornerShape(24.dp),
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
        )

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
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
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
                                    val selected = modelMatches(choice, currentSelection)
                                    val favoriteKey = modelFavoriteKey(group.instanceId, model.key)
                                    ModelRow(
                                        model = model,
                                        selected = selected,
                                        isFavorite = favorites.contains(favoriteKey),
                                        isHidden = hidden.contains(favoriteKey),
                                        manageHidden = manageHidden,
                                        onToggleFavorite = { toggleFavorite(favoriteKey) },
                                        onToggleHidden = { toggleHidden(favoriteKey) },
                                        onSelect = {
                                            onSelect(choice)
                                            onDismiss()
                                        },
                                    )
                                    if (index < group.models.lastIndex) {
                                        InsetDivider(start = 52.dp)
                                    }
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }

                if (onSelectRuntime != null) {
                    item(key = "options") {
                        Text(
                            text = "Options",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 6.dp),
                        )
                        GroupedCard {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { runtimeOpen = true }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Runtime",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = runtimeModeLabel(runtimeMode),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowRight,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }

                item(key = "catalog") {
                    Text(
                        text = "Catalog",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 6.dp),
                    )
                    GroupedCard {
                        CatalogToggleRow(
                            title = "Manage hidden models",
                            checked = manageHidden,
                            onCheckedChange = { manageHidden = it },
                        )
                        if (hasLegacy) {
                            InsetDivider(start = 16.dp)
                            CatalogToggleRow(
                                title = "Legacy models",
                                checked = showLegacy,
                                onCheckedChange = {
                                    showLegacy = it
                                    prefs.edit().putBoolean(MODEL_SHOW_LEGACY_KEY, it).apply()
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (runtimeOpen && onSelectRuntime != null) {
        AlertDialog(
            onDismissRequest = { runtimeOpen = false },
            title = { Text("Runtime") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    threadRuntimeOptions.forEach { mode ->
                        val selected = runtimeMode == mode.value
                        Surface(
                            onClick = {
                                onSelectRuntime(mode.value)
                                runtimeOpen = false
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                Color.Transparent
                            },
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = null,
                                )
                                Spacer(Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mode.label,
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                    Text(
                                        text = mode.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { runtimeOpen = false }) { Text("Close") }
            },
        )
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
            modifier = Modifier.size(15.dp),
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
    selected: Boolean,
    isFavorite: Boolean,
    isHidden: Boolean,
    manageHidden: Boolean,
    onToggleFavorite: () -> Unit,
    onToggleHidden: () -> Unit,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(start = 10.dp, end = 2.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
        )
        Spacer(Modifier.width(4.dp))
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = model.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (isHidden) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
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
        if (manageHidden) {
            IconButton(
                onClick = onToggleHidden,
                modifier = Modifier.size(38.dp),
            ) {
                Icon(
                    imageVector = if (isHidden) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = if (isHidden) "Show model" else "Hide model",
                    modifier = Modifier.size(19.dp),
                    tint = if (isHidden) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
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
                modifier = Modifier.size(20.dp),
                tint = if (isFavorite) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                },
            )
        }
    }
}

@Composable
private fun CatalogToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
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
