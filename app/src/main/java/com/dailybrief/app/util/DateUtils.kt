package com.dailybrief.app.util

import android.content.Context
import com.dailybrief.app.R
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {

    private val feedFormatters = listOf(
        DateTimeFormatter.RFC_1123_DATE_TIME,
        DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US),
        DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z", Locale.US),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.US),
        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss", Locale.US),
        DateTimeFormatter.ISO_OFFSET_DATE_TIME,
        DateTimeFormatter.ISO_LOCAL_DATE_TIME,
        DateTimeFormatter.ISO_INSTANT
    )
    private val localDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US)
    private val dateLabelFormatter = DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA)
    private val shortDateFormatter = DateTimeFormatter.ofPattern("MM-dd", Locale.CHINA)
    private val fullDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.CHINA)
    private val clockFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)
    private val detailFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm", Locale.CHINA)

    /** Parses the common RFC 822 / ISO 8601 variants used by news feeds. */
    fun parseFeedDate(raw: String?): Long? {
        val value = raw?.trim()
            ?.replace(Regex("\\s+"), " ")
            ?.takeIf { it.isNotEmpty() } ?: return null
        for (formatter in feedFormatters) {
            parseWith(formatter, value)?.let { return it }
        }
        parseWith(localDateTimeFormatter, value)?.let { return it }
        return null
    }

    private fun parseWith(formatter: DateTimeFormatter, value: String): Long? {
        val temporal = runCatching { formatter.parse(value) }.getOrNull() ?: return null
        runCatching { Instant.from(temporal).toEpochMilli() }.getOrNull()?.let { return it }
        return runCatching {
            LocalDateTime.from(temporal)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }.getOrNull()
    }

    fun todayLabel(now: Long = System.currentTimeMillis()): String =
        Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate().format(dateLabelFormatter)

    fun relativeTime(context: Context, epochMillis: Long, now: Long = System.currentTimeMillis()): String {
        if (epochMillis <= 0L) return context.getString(R.string.unknown_time)
        val zone = ZoneId.systemDefault()
        val then = Instant.ofEpochMilli(epochMillis).atZone(zone)
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val diff = (now - epochMillis).coerceAtLeast(0L)

        return when {
            then.toLocalDate() == today && diff < 60_000L ->
                context.getString(R.string.time_just_now)
            then.toLocalDate() == today && diff < 3_600_000L ->
                context.getString(R.string.time_minutes_ago, (diff / 60_000L).toInt().coerceAtLeast(1))
            then.toLocalDate() == today ->
                context.getString(R.string.time_hours_ago, (diff / 3_600_000L).toInt().coerceAtLeast(1))
            then.toLocalDate() == today.minusDays(1) ->
                context.getString(R.string.time_yesterday, then.format(clockFormatter))
            then.year == today.year ->
                then.format(shortDateFormatter)
            else ->
                then.format(fullDateFormatter)
        }
    }

    fun detailTime(epochMillis: Long): String? {
        if (epochMillis <= 0L) return null
        return Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .format(detailFormatter)
    }

    fun shortClock(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .format(clockFormatter)

    fun startOfToday(now: Long = System.currentTimeMillis()): LocalDate =
        Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate()
}
