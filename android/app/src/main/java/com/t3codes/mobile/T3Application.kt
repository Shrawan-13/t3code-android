package com.t3codes.mobile

import android.app.Application

class T3Application : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashReporter.install(this)
    }
}
