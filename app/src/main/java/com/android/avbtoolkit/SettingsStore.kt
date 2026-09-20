package com.android.avbtoolkit

import android.content.Context

data class SettingsUiState(
    val showFunctionKeyboard: Boolean = true,
)

/**
 * App-level settings backed by the shared "settings" SharedPreferences file
 * (same file the template's SettingsRepositoryImpl uses).
 */
class SettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var showFunctionKeyboard: Boolean
        get() = prefs.getBoolean("console_show_function_keyboard", true)
        set(value) = prefs.edit().putBoolean("console_show_function_keyboard", value).apply()
}
