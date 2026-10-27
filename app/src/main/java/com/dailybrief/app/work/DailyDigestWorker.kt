package com.dailybrief.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dailybrief.app.data.NewsCache
import com.dailybrief.app.data.NewsRepository
import com.dailybrief.app.data.SettingsStore
import com.dailybrief.app.util.NotificationHelper
import kotlinx.coroutines.CancellationException
import java.time.LocalDate

class DailyDigestWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val settings = SettingsStore(applicationContext)
        if (!settings.dailyDigestEnabled) return Result.success()

        val today = LocalDate.now().toString()
        if (settings.lastDigestDay == today) return Result.success()

        val repository = NewsRepository(
            NewsCache(applicationContext),
            settings
        )
        return try {
            val outcome = repository.refresh()
            if (outcome.articles.isEmpty()) {
                Result.retry()
            } else {
                NotificationHelper.showDailyDigest(applicationContext, outcome.articles)
                settings.lastDigestDay = today
                Result.success()
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
