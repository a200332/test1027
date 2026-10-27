package com.dailybrief.app

import android.app.Application
import com.dailybrief.app.data.SettingsStore
import com.dailybrief.app.util.DigestScheduler
import com.dailybrief.app.util.NotificationHelper

class DailyNewsApp : Application() {

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
        if (SettingsStore(this).dailyDigestEnabled) {
            DigestScheduler.schedule(this)
        }
    }
}
