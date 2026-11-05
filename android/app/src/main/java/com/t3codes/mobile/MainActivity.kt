package com.t3codes.mobile

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    private var launchThreadId by mutableStateOf<String?>(null)
    private var viewModel: MainViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pendingCrash = CrashReporter.takePending(this)
        if (pendingCrash != null) {
            CrashReporter.launchCrashScreen(this, pendingCrash)
            finish()
            return
        }
        launchThreadId = intent?.getStringExtra(T3Notifications.EXTRA_THREAD_ID)
        enableEdgeToEdge()
        setContent {
            val vm: MainViewModel = viewModel()
            viewModel = vm
            LaunchedEffect(launchThreadId) {
                launchThreadId?.let { threadId ->
                    launchThreadId = null
                    vm.openThreadFromNotification(threadId)
                }
            }
            T3Theme(themePref = vm.state.themePref) {
                T3App(vm)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(T3Notifications.EXTRA_THREAD_ID)?.let { threadId ->
            val vm = viewModel
            if (vm != null) {
                vm.openThreadFromNotification(threadId)
            } else {
                launchThreadId = threadId
            }
        }
    }

    override fun onStart() {
        super.onStart()
        AppVisibility.foreground = true
        viewModel?.startBackgroundSyncIfEnabled()
    }

    override fun onStop() {
        AppVisibility.foreground = false
        super.onStop()
    }
}
