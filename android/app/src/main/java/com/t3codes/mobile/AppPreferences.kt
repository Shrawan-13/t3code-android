package com.t3codes.mobile

import android.content.Context
import android.content.SharedPreferences

/** Keys shared by the UI and the optional background sync service. */
object AppPreferences {
    const val NAME = "t3_connection"
    const val BASE_URL = "base_url"
    const val TOKEN = "token"
    const val AUTO_CONNECT = "auto_connect"
    const val THEME = "theme_pref"
    const val BACKGROUND_SYNC = "background_sync"

    fun of(context: Context): SharedPreferences =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
}
