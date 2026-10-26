package com.t3codes.mobile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun T3App(viewModel: MainViewModel) {
    val state = viewModel.state
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
    BackHandler(
        enabled = !state.newThreadOpen &&
            state.terminal == null &&
            state.diff == null &&
            state.files == null &&
            state.activeThreadId == null &&
            state.connected &&
            state.tab != AppTab.Threads &&
            state.tab !in bottomTabs,
    ) { viewModel.selectTab(AppTab.Threads) }
    // The pull request detail sits above whichever list or thread opened it.
    BackHandler(enabled = state.pullRequestDetail != null) { viewModel.closePullRequest() }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            !state.connected -> ConnectScreen(state, viewModel)
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
private fun HomeScaffold(state: UiState, viewModel: MainViewModel) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (state.tab in bottomTabs) {
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
                                    imageVector = when (tab) {
                                        AppTab.PullRequests -> Icons.Rounded.CallMerge
                                        else -> Icons.Rounded.Forum
                                    },
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
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (state.tab) {
                AppTab.Threads -> ThreadsScreen(state, viewModel)
                AppTab.PullRequests -> PullRequestsScreen(state, viewModel)
                AppTab.Projects -> ProjectsScreen(state, viewModel)
                AppTab.Usage -> UsageScreen(state, viewModel)
                AppTab.Settings -> SettingsScreen(state, viewModel)
            }
        }
    }
}
