package com.dailybrief.app.data

import android.content.Context

class SettingsStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("daily_brief_settings", Context.MODE_PRIVATE)

    var enabledSourceIds: Set<String>
        get() {
            val stored = prefs.getStringSet(KEY_ENABLED_SOURCES, null)
            return stored?.filter { NewsSources.find(it) != null }?.toSet()
                ?.takeIf { it.isNotEmpty() }
                ?: NewsSources.defaultEnabledIds
        }
        set(value) {
            prefs.edit().putStringSet(KEY_ENABLED_SOURCES, value.toSet()).apply()
        }

    var dailyDigestEnabled: Boolean
        get() = prefs.getBoolean(KEY_DAILY_DIGEST, true)
        set(value) {
            prefs.edit().putBoolean(KEY_DAILY_DIGEST, value).apply()
        }

    var notificationAsked: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATION_ASKED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_NOTIFICATION_ASKED, value).apply()
        }

    var lastDigestDay: String?
        get() = prefs.getString(KEY_LAST_DIGEST_DAY, null)
        set(value) {
            prefs.edit().putString(KEY_LAST_DIGEST_DAY, value).apply()
        }

    fun setSourceEnabled(sourceId: String, enabled: Boolean): Set<String> {
        val current = enabledSourceIds
        val updated = if (enabled) current + sourceId else current - sourceId
        if (updated.isNotEmpty()) {
            enabledSourceIds = updated
        }
        return updated
    }

    private companion object {
        const val KEY_ENABLED_SOURCES = "enabled_sources"
        const val KEY_DAILY_DIGEST = "daily_digest"
        const val KEY_NOTIFICATION_ASKED = "notification_asked"
        const val KEY_LAST_DIGEST_DAY = "last_digest_day"
    }
}
