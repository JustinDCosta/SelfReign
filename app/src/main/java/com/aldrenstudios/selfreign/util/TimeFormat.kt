package com.aldrenstudios.selfreign.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Formatting helpers for durations and timestamps. */
object TimeFormat {

    /** Breaks a duration in millis into days / hours / minutes / seconds. */
    data class Parts(val days: Long, val hours: Long, val minutes: Long, val seconds: Long)

    fun parts(durationMillis: Long): Parts {
        val safe = durationMillis.coerceAtLeast(0)
        val days = TimeUnit.MILLISECONDS.toDays(safe)
        val hours = TimeUnit.MILLISECONDS.toHours(safe) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(safe) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(safe) % 60
        return Parts(days, hours, minutes, seconds)
    }

    /** Compact human summary, e.g. "3d 4h" or "12m 5s". Used for the best streak. */
    fun shortDuration(durationMillis: Long): String {
        val p = parts(durationMillis)
        return when {
            p.days > 0 -> "${p.days}d ${p.hours}h"
            p.hours > 0 -> "${p.hours}h ${p.minutes}m"
            p.minutes > 0 -> "${p.minutes}m ${p.seconds}s"
            else -> "${p.seconds}s"
        }
    }

    private val dateTimeFormat = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())

    /** Formats an epoch-millis timestamp for the history list. */
    fun dateTime(epochMillis: Long): String = dateTimeFormat.format(Date(epochMillis))
}
