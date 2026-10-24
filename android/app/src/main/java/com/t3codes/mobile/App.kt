package com.t3codes.mobile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun T3App(viewModel: MainViewModel) {
    val state = viewModel.state
    val snackbar = remember { SnackbarHostState() }

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

    BackHandler(enabled = state.terminal != null) { viewModel.closeTerminal() }
    BackHandler(enabled = state.terminal == null && state.diff != null) { viewModel.closeDiff() }
    BackHandler(
        enabled = state.terminal == null && state.diff == null &&
            state.files?.file != null,
    ) { viewModel.closeFile() }
    BackHandler(
        enabled = state.terminal == null && state.diff == null &&
            state.files != null && state.files.file == null,
    ) { viewModel.closeFiles() }
    BackHandler(
        enabled = state.terminal == null && state.diff == null && state.files == null &&
            state.activeThreadId != null,
    ) { viewModel.closeThread() }
    BackHandler(
        enabled = state.terminal == null &&
            state.diff == null &&
            state.files == null &&
            state.activeThreadId == null &&
            state.connected &&
            state.tab != AppTab.Threads,
    ) { viewModel.selectTab(AppTab.Threads) }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            !state.connected -> ConnectScreen(state, viewModel)
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

    if (state.newThreadOpen && state.connected) {
        NewThreadSheet(state, viewModel)
    }
}

@Composable
private fun HomeScaffold(state: UiState, viewModel: MainViewModel) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 0.dp,
            ) {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = state.tab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        icon = {
                            Icon(
                                imageVector = when (tab) {
                                    AppTab.Threads -> Icons.Rounded.Forum
                                    AppTab.Projects -> Icons.Rounded.FolderOpen
                                    AppTab.Usage -> Icons.Rounded.BarChart
                                    AppTab.Settings -> Icons.Rounded.Settings
                                },
                                contentDescription = tab.label,
                            )
                        },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
        floatingActionButton = {
            if (state.tab == AppTab.Threads) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.openNewThread() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("New thread") },
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (state.tab) {
                AppTab.Threads -> ThreadsScreen(state, viewModel)
                AppTab.Projects -> ProjectsScreen(state, viewModel)
                AppTab.Usage -> UsageScreen(state, viewModel)
                AppTab.Settings -> SettingsScreen(state, viewModel)
            }
        }
    }
}
