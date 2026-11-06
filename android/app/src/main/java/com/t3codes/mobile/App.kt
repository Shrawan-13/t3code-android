package com.t3codes.mobile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CallMerge
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

private fun visibleNavTabs(order: List<String>, hidden: List<String>): List<AppTab> =
    order.filterNot { it in hidden }.mapNotNull { id ->
        when (id) {
            "threads" -> AppTab.Threads
            "pullRequests" -> AppTab.PullRequests
            "usage" -> AppTab.Usage
            else -> null
        }
    }

private fun navTabIcon(tab: AppTab): androidx.compose.ui.graphics.vector.ImageVector =
    when (tab) {
        AppTab.PullRequests -> Icons.Rounded.CallMerge
        AppTab.Usage -> Icons.Rounded.BarChart
        else -> Icons.Rounded.Forum
    }

@Composable
fun T3App(viewModel: MainViewModel) {
    val state = viewModel.state
    val bottomTabs = remember(state.navTabOrder, state.navHiddenTabs) {
        visibleNavTabs(state.navTabOrder, state.navHiddenTabs)
    }
    val snackbar = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    AppVisibility.foreground = true
                    viewModel.onAppForegrounded()
                }
                Lifecycle.Event.ON_STOP -> {
                    AppVisibility.foreground = false
                    viewModel.onAppBackgrounded()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state.toast) {
        state.toast?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeToast()
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbar.showSnackbar(it)
            if (state.connected) viewModel.clearError()
        }
    }

    BackHandler(enabled = state.newThreadOpen) { viewModel.closeNewThread() }
    BackHandler(enabled = !state.newThreadOpen && state.terminal != null) { viewModel.closeTerminal() }
    BackHandler(
        enabled = !state.newThreadOpen && state.terminal == null && state.diff != null,
    ) { viewModel.closeDiff() }
    BackHandler(
        enabled = !state.newThreadOpen && state.terminal == null && state.diff == null &&
            state.files?.file != null,
    ) { viewModel.closeFile() }
    BackHandler(
        enabled = !state.newThreadOpen && state.terminal == null && state.diff == null &&
            state.files != null && state.files.file == null,
    ) { viewModel.closeFiles() }
    BackHandler(
        enabled = !state.newThreadOpen && state.terminal == null && state.diff == null &&
            state.files == null && state.activeThreadId != null,
    ) { viewModel.closeThread() }
    BackHandler(enabled = state.settingsPage != null) { viewModel.closeSettingsPage() }
    BackHandler(
        enabled = !state.newThreadOpen &&
            state.terminal == null &&
            state.diff == null &&
            state.files == null &&
            state.activeThreadId == null &&
            state.connected &&
            state.tab != AppTab.Threads &&
            state.settingsPage == null &&
            state.tab !in bottomTabs,
    ) { viewModel.selectTab(AppTab.Threads) }
    // The pull request detail sits above whichever list or thread opened it.
    BackHandler(enabled = state.pullRequestDetail != null) { viewModel.closePullRequest() }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            !state.connected -> ConnectScreen(state, viewModel)
            state.addProjectOpen -> AddProjectScreen(state, viewModel)
            state.newThreadOpen -> NewThreadScreen(state, viewModel)
            state.pullRequestDetail != null -> PullRequestDetailScreen(state, viewModel)
            state.terminal != null -> TerminalScreen(state, viewModel)
            state.diff != null -> DiffScreen(state, viewModel)
            state.files != null -> FilesScreen(state, viewModel)
            state.activeThreadId != null && state.thread != null -> ThreadScreen(state, viewModel)
            else -> HomeScaffold(state, viewModel)
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 96.dp),
        )
    }
}

@Composable
private fun OneUiNavBar(
    tabs: List<AppTab>,
    selected: AppTab,
    onSelect: (AppTab) -> Unit,
    onCompose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEach { tab ->
                    val active = tab == selected
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onSelect(tab) }
                            .padding(vertical = 7.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Icon(
                            imageVector = navTabIcon(tab),
                            contentDescription = tab.label,
                            modifier = Modifier.size(20.dp),
                            tint = if (active) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (active) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = onCompose,
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "New thread")
        }
    }
}

@Composable
private fun HomeScaffold(state: UiState, viewModel: MainViewModel) {
    val bottomTabs = remember(state.navTabOrder, state.navHiddenTabs) {
        visibleNavTabs(state.navTabOrder, state.navHiddenTabs)
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (state.tab in bottomTabs) {
                if (state.androidNavStyle == "one-ui") {
                    OneUiNavBar(
                        tabs = bottomTabs,
                        selected = state.tab,
                        onSelect = viewModel::selectTab,
                        onCompose = { viewModel.openNewThread() },
                    )
                } else {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        tonalElevation = 0.dp,
                    ) {
                        bottomTabs.forEach { tab ->
                            NavigationBarItem(
                                selected = state.tab == tab,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = navTabIcon(tab),
                                        contentDescription = tab.label,
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (state.tab == AppTab.Threads) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FloatingActionButton(
                        onClick = { viewModel.setHomeFilterOpen(true) },
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ) {
                        Icon(Icons.Rounded.FilterList, contentDescription = "Filter chats")
                    }
                    if (state.androidNavStyle != "one-ui") {
                        ExtendedFloatingActionButton(
                            onClick = { viewModel.openNewThread() },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                            text = {
                                Text(
                                    "New thread",
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (state.tab) {
                AppTab.Threads -> ThreadsScreen(state, viewModel)
                AppTab.PullRequests -> PullRequestsScreen(state, viewModel)
                AppTab.Projects -> ProjectsScreen(state, viewModel)
                AppTab.Usage -> UsageScreen(state, viewModel)
                AppTab.Settings -> SettingsRoot(state, viewModel)
            }
        }
    }
}
