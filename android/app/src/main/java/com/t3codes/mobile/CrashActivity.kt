package com.t3codes.mobile

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CrashActivity : ComponentActivity() {
    companion object {
        const val EXTRA_CRASH_PATH = "crash_path"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Clear the pending marker as soon as the screen is shown so a normal
        // restart does not keep reopening an old crash.
        val pending = CrashReporter.takePending(this)
        val file = intent.getStringExtra(EXTRA_CRASH_PATH)
            ?.let(::File)
            ?.takeIf { it.isFile }
            ?: pending

        setContent {
            T3Theme(themePref = themePreference()) {
                CrashScreen(
                    file = file,
                    onShare = { file?.let { CrashReporter.share(this, it) } },
                    onCopy = { file?.let { CrashReporter.copy(this, it) } },
                    onRestart = ::restartApp,
                    onClose = ::closeScreen,
                )
            }
        }
    }

    private fun restartApp() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            },
        )
        finish()
    }

    private fun closeScreen() {
        finishAndRemoveTask()
    }

    private fun themePreference(): ThemePref {
        val prefs = getSharedPreferences("t3_connection", Context.MODE_PRIVATE)
        return runCatching {
            ThemePref.valueOf(
                prefs.getString("theme_pref", ThemePref.System.name) ?: ThemePref.System.name,
            )
        }.getOrDefault(ThemePref.System)
    }
}

@Composable
private fun CrashScreen(
    file: File?,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onRestart: () -> Unit,
    onClose: () -> Unit,
) {
    val preview = remember(file) {
        file?.let { runCatching { it.readText() }.getOrNull() }.orEmpty()
    }
    val timestamp = remember(file) {
        file?.lastModified()?.takeIf { it > 0 }?.let {
            SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault()).format(Date(it))
        }
    }
    val device = remember {
        "${Build.MANUFACTURER} ${Build.MODEL}".trim()
    }
    val version = remember {
        "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    }
    val fatal = remember(preview) {
        val lines = preview.lineSequence().toList()
        val start = lines.indexOfFirst { it.startsWith("Fatal exception:") }
        if (start >= 0) {
            lines.drop(start + 1).firstOrNull { it.isNotBlank() }?.trim().orEmpty()
        } else {
            ""
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "CRASH REPORT",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close crash report")
                }
            }

            Spacer(Modifier.height(30.dp))
            Surface(
                shape = RoundedCornerShape(30.dp),
                color = MaterialTheme.colorScheme.errorContainer,
            ) {
                Box(modifier = Modifier.size(92.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
            Text(
                text = "T3 Code Android crashed",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(9.dp))
            Text(
                text = "Something unexpected stopped the app. The full crash log is ready to share with the developer.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))

            if (fatal.isNotBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Text(
                        text = fatal,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
                Spacer(Modifier.height(14.dp))
            }

            if (file != null) {
                GroupedCard {
                    CrashInfoRow("Saved", timestamp ?: "Just now")
                    InsetDivider(start = 18.dp)
                    CrashInfoRow("Version", version)
                    InsetDivider(start = 18.dp)
                    CrashInfoRow("Device", device)
                    InsetDivider(start = 18.dp)
                    CrashInfoRow("Log file", file.name)
                }
                Spacer(Modifier.height(18.dp))
                SectionLabel("Crash log preview")
                Spacer(Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    SelectionContainer {
                        Text(
                            text = preview.ifBlank { "The crash log is empty." },
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .heightIn(max = 270.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${formatBytes(file.length())} · saved on this device",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                ErrorBanner(
                    message = "The crash log could not be saved, but you can still restart the app and try again.",
                )
            }

            Spacer(Modifier.height(22.dp))
            Button(
                onClick = onShare,
                enabled = file != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = CircleShape,
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
                Text("Share log file")
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onCopy,
                    enabled = file != null,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = CircleShape,
                ) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("Copy")
                }
                OutlinedButton(
                    onClick = onRestart,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = CircleShape,
                ) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("Restart")
                }
            }
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                Text("Close")
            }
        }
    }
}

@Composable
private fun CrashInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(78.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
}
