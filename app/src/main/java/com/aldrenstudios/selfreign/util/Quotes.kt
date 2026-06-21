package com.aldrenstudios.selfreign.util

import java.util.concurrent.TimeUnit

/**
 * A small curated set of motivational quotes. A quote is chosen deterministically
 * from the current day so it stays stable for the whole day but rotates daily.
 */
object Quotes {

    private val quotes = listOf(
        "Every moment you resist is a vote for the person you want to become.",
        "Progress, not perfection. One clean hour at a time.",
        "The urge will pass whether you act on it or not. Let it pass.",
        "You've already done the hardest part: deciding to change.",
        "Falling down is part of it. Staying down isn't.",
        "Your future self is watching. Make them proud.",
        "Discipline is choosing what you want most over what you want now.",
        "A slip is a comma, not a full stop. Keep writing your story.",
        "Small steps every day add up to big change.",
        "Crave the freedom more than the habit.",
        "You are not your urges. You are the one who observes them.",
        "The best time to restart was a minute ago. The next best time is now."
    )

    /** Returns the quote for a given day (defaults to today). */
    fun forDay(epochMillis: Long = System.currentTimeMillis()): String {
        val dayIndex = TimeUnit.MILLISECONDS.toDays(epochMillis)
        val idx = (dayIndex % quotes.size).toInt()
        return quotes[idx]
    }

    /** Total number of quotes (for cycling by index). */
    val count: Int get() = quotes.size

    /** Returns the quote at [index], wrapping around safely. */
    fun byIndex(index: Int): String = quotes[((index % count) + count) % count]
}
