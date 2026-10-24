package com.t3codes.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.t3codes.core.ServerConfig
import com.t3codes.core.ThreadShell

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            T3Theme {
                val viewModel: MainViewModel = viewModel()
                CoreScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoreScreen(viewModel: MainViewModel) {
    val state = viewModel.state
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.error) {
        state.error?.let { snackbar.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("T3 Code core") },
                actions = {
                    if (state.config != null) {
                        IconButton(onClick = viewModel::refreshThreads) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh threads")
                        }
                        IconButton(onClick = viewModel::disconnect) {
                            Icon(Icons.Filled.Close, contentDescription = "Disconnect")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ConnectionCard(
                    state = state,
                    onBaseUrlChange = viewModel::onBaseUrlChange,
                    onTokenChange = viewModel::onTokenChange,
                    onPairingLinkChange = viewModel::onPairingLinkChange,
                    onConnect = viewModel::connect,
                    onPair = viewModel::pair,
                    onSelfTest = viewModel::runSelfTest,
                )
            }

            state.config?.let { config ->
                item { EnvironmentCard(config) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Stat("Projects", state.projectCount.toString())
                        Stat("Threads", state.threads.size.toString())
                        Stat("Archived", state.archivedCount.toString())
                    }
                }
            }

            if (state.threads.isNotEmpty()) {
                item { SectionTitle("Active threads") }
                items(state.threads, key = { it.id }) { thread -> ThreadRow(thread) }
            }

            if (state.log.isNotEmpty()) {
                item { SectionTitle("Core self-test") }
                items(state.log) { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionCard(
    state: UiState,
    onBaseUrlChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onPairingLinkChange: (String) -> Unit,
    onConnect: () -> Unit,
    onPair: () -> Unit,
    onSelfTest: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Connect", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = state.baseUrl,
                onValueChange = onBaseUrlChange,
                label = { Text("Server URL") },
                singleLine = true,
                placeholder = { Text("http://192.168.1.20:3773") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.token,
                onValueChange = onTokenChange,
                label = { Text("Bearer token") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.pairingLink,
                onValueChange = onPairingLinkChange,
                label = { Text("Pairing link (optional)") },
                singleLine = true,
                placeholder = { Text("t3.codes/pair#token=…") },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(onClick = onConnect, enabled = !state.busy) {
                    Text(if (state.config == null) "Connect" else "Reconnect")
                }
                OutlinedButton(onClick = onPair, enabled = !state.busy) {
                    Text("Pair")
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onSelfTest, enabled = state.config != null && !state.busy) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Run core self-test")
                }
            }
            if (state.busy) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.size(8.dp))
                    Text("Working…", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun EnvironmentCard(config: ServerConfig) {
    val environment = config.environment
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(environment.label.ifEmpty { "environment" }, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                AssistChip(onClick = {}, label = { Text("v${environment.serverVersion}") })
            }
            Text(
                text = "id ${environment.environmentId}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
            Text(
                text = "${environment.platform.os}/${environment.platform.arch} · protocol ${environment.orchestrationProtocolVersion}",
                style = MaterialTheme.typography.bodySmall,
            )
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            val providers = config.providers.joinToString { "${it.displayName.ifEmpty { it.instanceId }}=${it.status}" }
            Text(
                text = if (providers.isEmpty()) "no providers" else providers,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ThreadRow(thread: ThreadShell) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = thread.title.ifEmpty { thread.id },
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val status = thread.status.ifEmpty {
                when {
                    thread.isRunning -> "running"
                    thread.isPinned -> "pinned"
                    thread.isArchived -> "archived"
                    else -> ""
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (status.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Text(
                            text = status,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                    Spacer(Modifier.size(8.dp))
                }
                Text(
                    text = thread.modelSelection.model.ifEmpty { thread.modelSelection.instanceId },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Column {
        Spacer(Modifier.height(4.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}
