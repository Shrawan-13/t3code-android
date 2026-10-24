package com.t3codes.mobile

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val TerminalBackground = Color(0xFF0B0B10)
private val TerminalForeground = Color(0xFFDAD5EA)
private val TerminalDim = Color(0xFF8D889D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(state: UiState, viewModel: MainViewModel) {
    val terminal = state.terminal ?: return
    var input by rememberSaveable(terminal.threadId) { mutableStateOf("") }
    val lines = remember(terminal.text) { terminal.text.split('\n') }
    val listState = rememberLazyListState()
    val atBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            last >= info.totalItemsCount - 2
        }
    }
    val context = LocalContext.current
    val cwdLabel = terminal.cwd.substringAfterLast('/').ifBlank { terminal.cwd }

    fun submit() {
        val line = input
        if (line.isBlank()) return
        viewModel.terminalInput(line)
        input = ""
    }

    LaunchedEffect(lines.size) {
        if (atBottom && lines.isNotEmpty()) listState.scrollToItem(lines.lastIndex)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = TerminalBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TerminalBackground,
                    titleContentColor = TerminalForeground,
                ),
                navigationIcon = {
                    IconButton(onClick = viewModel::closeTerminal) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = TerminalForeground)
                    }
                },
                title = {
                    Column {
                        Text("Terminal", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = cwdLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = TerminalDim,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(ClipboardManager::class.java)
                            clipboard.setPrimaryClip(ClipData.newPlainText("Terminal", terminal.text))
                            Toast.makeText(context, "Terminal output copied", Toast.LENGTH_SHORT).show()
                        },
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy output", tint = TerminalForeground)
                    }
                },
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(
                        "Ctrl-C" to "ctrl-c",
                        "Tab" to "tab",
                        "Esc" to "esc",
                        "↑" to "up",
                        "↓" to "down",
                    ).forEach { (label, key) ->
                        AssistChip(
                            onClick = { viewModel.terminalKey(key) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            shape = CircleShape,
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Run a command…", style = MaterialTheme.typography.bodySmall) },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { submit() }),
                    )
                    Spacer(Modifier.width(5.dp))
                    IconButton(onClick = { submit() }, enabled = input.isNotBlank()) {
                        Icon(Icons.Rounded.Send, contentDescription = "Run command")
                    }
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(TerminalBackground),
        ) {
            if (terminal.text.isEmpty()) {
                if (terminal.error != null) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(22.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Rounded.Terminal,
                            contentDescription = null,
                            tint = TerminalDim,
                            modifier = Modifier.size(34.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = terminal.error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        TextButton(onClick = { viewModel.openTerminal(terminal.threadId) }) {
                            Text("Retry")
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(15.dp),
                            strokeWidth = 2.dp,
                            color = TerminalDim,
                        )
                        Text(
                            text = "Starting shell in $cwdLabel…",
                            color = TerminalDim,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    itemsIndexed(lines) { _, line ->
                        Text(
                            text = if (line.isEmpty()) " " else line,
                            color = TerminalForeground,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                            ),
                        )
                    }
                }
            }

            if (terminal.error != null && terminal.text.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(10.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ) {
                    Text(
                        text = terminal.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }
    }
}
