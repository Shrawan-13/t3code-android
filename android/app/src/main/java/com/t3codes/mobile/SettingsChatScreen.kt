package com.t3codes.mobile

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable

@Composable
fun SettingsChatScreen(state: UiState, viewModel: MainViewModel) {
    SettingsScaffold(
        title = "Chat",
        onBack = viewModel::closeSettingsPage,
    ) {
        SettingsSection(title = "While the agent is running") {
            Column {
                SettingsChoiceRow(
                    title = "Queue",
                    description = "Your message waits and runs after the current turn finishes.",
                    selected = state.followUpBehavior == "queue",
                    onClick = { viewModel.setFollowUpBehavior("queue") },
                )
                SettingsChoiceRow(
                    title = "Steer",
                    description = "Your message reaches the agent right away, changing what it is working on.",
                    selected = state.followUpBehavior == "steer",
                    separated = true,
                    onClick = { viewModel.setFollowUpBehavior("steer") },
                )
            }
        }
        SettingsNote(
            "Long-press the send button to use the other option for a single message. With a hardware keyboard, hold Ctrl while sending.",
        )
    }
}
