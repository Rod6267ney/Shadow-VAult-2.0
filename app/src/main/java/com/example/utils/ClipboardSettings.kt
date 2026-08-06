package com.example.utils

import android.content.Context
import android.content.SharedPreferences

object ClipboardSettings {
    private const val PREFS_NAME = "clipboard_isolation_settings"
    private const val KEY_ISOLATION_ENABLED = "clipboard_isolation_enabled"
    private const val KEY_AUTO_CLEAR_TIMEOUT = "clipboard_auto_clear_timeout_sec"
    private const val KEY_CLEAR_ON_BACKGROUND = "clipboard_clear_on_background"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isIsolationEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ISOLATION_ENABLED, true)
    }

    fun setIsolationEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ISOLATION_ENABLED, enabled).apply()
    }

    fun getAutoClearTimeoutSeconds(context: Context): Int {
        return getPrefs(context).getInt(KEY_AUTO_CLEAR_TIMEOUT, 30)
    }

    fun setAutoClearTimeoutSeconds(context: Context, seconds: Int) {
        getPrefs(context).edit().putInt(KEY_AUTO_CLEAR_TIMEOUT, seconds).apply()
    }

    fun isClearOnBackgroundEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_CLEAR_ON_BACKGROUND, true)
    }

    fun setClearOnBackgroundEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_CLEAR_ON_BACKGROUND, enabled).apply()
    }
}
