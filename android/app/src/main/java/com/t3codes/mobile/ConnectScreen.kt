package com.t3codes.mobile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ConnectScreen(state: UiState, viewModel: MainViewModel) {
    var showPairing by rememberSaveable { mutableStateOf(state.pairingLink.isNotBlank()) }
    var showToken by rememberSaveable { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        colors.primaryContainer.copy(alpha = 0.65f),
                        colors.background,
                        colors.background,
                    ),
                ),
            ),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = colors.primary.copy(alpha = 0.10f),
                radius = size.minDimension * 0.65f,
                center = Offset(size.width * 0.92f, size.height * 0.02f),
            )
            drawCircle(
                color = colors.tertiary.copy(alpha = 0.08f),
                radius = size.minDimension * 0.45f,
                center = Offset(size.width * 0.02f, size.height * 0.62f),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            Spacer(Modifier.height(34.dp))
            BrandMark(size = 66.dp)
            Spacer(Modifier.height(22.dp))
            Text(
                text = "T3 Code",
                style = MaterialTheme.typography.displayLarge,
                color = colors.onBackground,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Your agents, on your phone.",
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurfaceVariant,
                fontWeight = FontWeight.Normal,
            )
            Spacer(Modifier.height(30.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(30.dp),
                color = colors.surfaceContainerLow,
                tonalElevation = 1.dp,
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "Connect an environment",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = "Point this device at your T3 server.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }

                    OutlinedTextField(
                        value = state.baseUrl,
                        onValueChange = viewModel::onBaseUrlChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Server URL") },
                        placeholder = { Text("http://192.168.1.20:3773") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Dns, contentDescription = null)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next,
                        ),
                        colors = fieldColors(),
                    )

                    OutlinedTextField(
                        value = state.token,
                        onValueChange = viewModel::onTokenChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Access token") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Key, contentDescription = null)
                        },
                        trailingIcon = {
                            IconButton(onClick = { showToken = !showToken }) {
                                Icon(
                                    imageVector = if (showToken) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = if (showToken) "Hide token" else "Show token",
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        visualTransformation = if (showToken) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        colors = fieldColors(),
                    )

                    AnimatedVisibility(visible = showPairing) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = state.pairingLink,
                                onValueChange = viewModel::onPairingLinkChange,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Pairing link or code") },
                                placeholder = { Text("t3.codes/pair#…") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = fieldColors(),
                            )
                            Button(
                                onClick = viewModel::pair,
                                enabled = !state.connecting && state.pairingLink.isNotBlank(),
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Text("Pair this device")
                            }
                        }
                    }

                    if (state.error != null) {
                        ErrorBanner(message = state.error, onDismiss = viewModel::clearError)
                    }

                    Button(
                        onClick = viewModel::connect,
                        enabled = !state.connecting,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        if (state.connecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(Modifier.size(10.dp))
                            Text("Connecting…")
                        } else {
                            Text("Connect", style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = { showPairing = !showPairing }) {
                            Text(if (showPairing) "Use a token instead" else "Pair with a link")
                        }
                        Spacer(Modifier.weight(1f))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = colors.onSurfaceVariant,
                            )
                            Text(
                                text = "on-device",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                text = "The Go core speaks the T3 orchestration protocol directly. " +
                    "Your token never leaves this device.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
)
