package com.t3codes.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pendingCrash = CrashReporter.takePending(this)
        if (pendingCrash != null) {
            CrashReporter.launchCrashScreen(this, pendingCrash)
            finish()
            return
        }
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            T3Theme(themePref = viewModel.state.themePref) {
                T3App(viewModel)
            }
        }
    }
}
