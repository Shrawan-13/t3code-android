package com.t3codes.mobile

import android.content.Context
import android.content.SharedPreferences

/**
 * Typed facade over the app's SharedPreferences. Everything the official client keeps in
 * device-local preferences lives here; server-synced settings are [ServerSettings].
 */
object AppSettings {
    const val NAME = "t3_connection"

    private const val THEME_MODE = "theme_mode"
    private const val THEME_LEGACY = "theme_pref"
    private const val THEME_LIGHT_ID = "theme_light_id"
    private const val THEME_DARK_ID = "theme_dark_id"
    private const val ANDROID_NAV_STYLE = "android_nav_style"
    private const val BASE_FONT_SIZE = "base_font_size"
    private const val TERMINAL_FONT_SIZE = "terminal_font_size"
    private const val CODE_FONT_SIZE = "code_font_size"
    private const val CODE_WORD_BREAK = "code_word_break"
    private const val MATH_RENDERING = "math_rendering_enabled"
    private const val WORKING_SHELF = "working_shelf_enabled"
    private const val USE_LAST_PROJECT = "use_last_project_for_new_threads"
    private const val PROJECT_GROUPING_MODE = "project_grouping_mode"
    private const val FOLLOW_UP_BEHAVIOR = "follow_up_behavior"
    private const val CRASH_REPORTS = "crash_reports_enabled"
    private const val NAV_TAB_ORDER = "navigation_tab_order"
    private const val HIDDEN_NAV_TABS = "hidden_navigation_tabs"
    private const val PLAN_MODE_LEGACY = "plan_mode_enabled"
    private const val MICROPHONE_ORDER = "microphone_order"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    // region Theme & appearance

    fun themeMode(context: Context): String {
        // Migrate the legacy "System|Light|Dark" key written by ThemePref.
        val legacy = prefs(context).getString(THEME_LEGACY, null)
        val stored = prefs(context).getString(THEME_MODE, null) ?: legacy?.lowercase()
        return when (stored) {
            "light", "dark", "system" -> stored
            else -> "system"
        }
    }

    fun setThemeMode(context: Context, mode: String) {
        prefs(context).edit()
            .putString(THEME_MODE, mode.lowercase())
            .remove(THEME_LEGACY)
            .apply()
    }

    fun lightThemeId(context: Context): String =
        prefs(context).getString(THEME_LIGHT_ID, null) ?: "t3-code"

    fun darkThemeId(context: Context): String =
        prefs(context).getString(THEME_DARK_ID, null) ?: "t3-code"

    fun setThemeId(context: Context, appearance: String, id: String) {
        prefs(context).edit()
            .putString(if (appearance == "light") THEME_LIGHT_ID else THEME_DARK_ID, id)
            .apply()
    }

    fun setThemeIdBoth(context: Context, id: String) {
        prefs(context).edit()
            .putString(THEME_LIGHT_ID, id)
            .putString(THEME_DARK_ID, id)
            .apply()
    }

    fun androidNavStyle(context: Context): String {
        val stored = prefs(context).getString(ANDROID_NAV_STYLE, null)
        if (stored == "one-ui" || stored == "material") return stored
        // Samsung defaults to One UI, everyone else to Material.
        val manufacturer = android.os.Build.MANUFACTURER.orEmpty().lowercase()
        return if (manufacturer == "samsung") "one-ui" else "material"
    }

    fun setAndroidNavStyle(context: Context, style: String) {
        prefs(context).edit().putString(ANDROID_NAV_STYLE, style).apply()
    }

    const val MIN_BASE_FONT_SIZE = 11
    const val MAX_BASE_FONT_SIZE = 22
    const val DEFAULT_BASE_FONT_SIZE = 16

    fun baseFontSize(context: Context): Int =
        prefs(context).getInt(BASE_FONT_SIZE, DEFAULT_BASE_FONT_SIZE)
            .coerceIn(MIN_BASE_FONT_SIZE, MAX_BASE_FONT_SIZE)

    fun setBaseFontSize(context: Context, value: Int) {
        prefs(context).edit()
            .putInt(BASE_FONT_SIZE, value.coerceIn(MIN_BASE_FONT_SIZE, MAX_BASE_FONT_SIZE))
            .apply()
    }

    /** Null means "derive from baseFontSize", matching the official client. */
    fun terminalFontSize(context: Context): Float? =
        prefs(context).takeIf { it.contains(TERMINAL_FONT_SIZE) }
            ?.getFloat(TERMINAL_FONT_SIZE, 0f)

    fun setTerminalFontSize(context: Context, value: Float?) {
        prefs(context).edit().apply {
            if (value == null) remove(TERMINAL_FONT_SIZE) else putFloat(TERMINAL_FONT_SIZE, value)
        }.apply()
    }

    fun codeFontSize(context: Context): Int? =
        prefs(context).takeIf { it.contains(CODE_FONT_SIZE) }
            ?.getInt(CODE_FONT_SIZE, 0)

    fun setCodeFontSize(context: Context, value: Int?) {
        prefs(context).edit().apply {
            if (value == null) remove(CODE_FONT_SIZE) else putInt(CODE_FONT_SIZE, value.coerceIn(8, 18))
        }.apply()
    }

    fun codeWordBreak(context: Context): Boolean =
        prefs(context).getBoolean(CODE_WORD_BREAK, false)

    fun setCodeWordBreak(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(CODE_WORD_BREAK, value).apply()
    }

    fun mathRenderingEnabled(context: Context): Boolean =
        prefs(context).getBoolean(MATH_RENDERING, false)

    fun setMathRenderingEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(MATH_RENDERING, value).apply()
    }

    // endregion

    // region Threads & projects

    fun workingShelfEnabled(context: Context): Boolean =
        prefs(context).getBoolean(WORKING_SHELF, true)

    fun setWorkingShelfEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(WORKING_SHELF, value).apply()
    }

    fun useLastProjectForNewThreads(context: Context): Boolean =
        prefs(context).getBoolean(USE_LAST_PROJECT, true)

    fun setUseLastProjectForNewThreads(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(USE_LAST_PROJECT, value).apply()
    }

    fun projectGroupingMode(context: Context): String =
        prefs(context).getString(PROJECT_GROUPING_MODE, "repository")!!

    fun setProjectGroupingMode(context: Context, mode: String) {
        prefs(context).edit().putString(PROJECT_GROUPING_MODE, mode).apply()
    }

    fun followUpBehavior(context: Context): String =
        prefs(context).getString(FOLLOW_UP_BEHAVIOR, "steer")!!

    fun setFollowUpBehavior(context: Context, value: String) {
        prefs(context).edit().putString(FOLLOW_UP_BEHAVIOR, value).apply()
    }

    /** Legacy Labs toggle: restore the Build/Plan mode control. */
    fun planModeLegacyEnabled(context: Context): Boolean =
        prefs(context).getBoolean(PLAN_MODE_LEGACY, false)

    fun setPlanModeLegacyEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(PLAN_MODE_LEGACY, value).apply()
    }

    // endregion

    // region Diagnostics & misc

    fun crashReportsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(CRASH_REPORTS, true)

    fun setCrashReportsEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(CRASH_REPORTS, value).apply()
    }

    // endregion

    // region Navigation tabs

    fun navigationTabOrder(context: Context): List<String> =
        prefs(context).getString(NAV_TAB_ORDER, null)
            ?.split(',')
            ?.filter { it.isNotBlank() }
            .orEmpty()

    fun setNavigationTabOrder(context: Context, order: List<String>) {
        prefs(context).edit().putString(NAV_TAB_ORDER, order.joinToString(",")).apply()
    }

    fun hiddenNavigationTabs(context: Context): List<String> =
        prefs(context).getString(HIDDEN_NAV_TABS, null)
            ?.split(',')
            ?.filter { it.isNotBlank() }
            .orEmpty()

    fun setHiddenNavigationTabs(context: Context, hidden: List<String>) {
        prefs(context).edit().putString(HIDDEN_NAV_TABS, hidden.joinToString(",")).apply()
    }

    fun resetNavigationTabs(context: Context) {
        prefs(context).edit().remove(NAV_TAB_ORDER).remove(HIDDEN_NAV_TABS).apply()
    }

    // endregion

    // region Microphone

    /** JSON array of microphone kinds, in preferred order. */
    fun microphoneOrder(context: Context): List<String> =
        prefs(context).getString(MICROPHONE_ORDER, null)
            ?.let { raw -> runCatching { T3Core.json.parseToJsonElement(raw) }.getOrNull() }
            ?.let { element ->
                (element as? kotlinx.serialization.json.JsonArray)
                    ?.mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
            }
            .orEmpty()

    fun setMicrophoneOrder(context: Context, kinds: List<String>) {
        val json = kotlinx.serialization.json.buildJsonArray { kinds.forEach { add(it) } }.toString()
        prefs(context).edit().putString(MICROPHONE_ORDER, json).apply()
    }

    // endregion
}
