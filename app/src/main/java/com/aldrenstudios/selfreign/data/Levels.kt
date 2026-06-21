package com.aldrenstudios.selfreign.data

import java.util.concurrent.TimeUnit

/**
 * A single milestone in the recovery journey.
 *
 * @param level         The level number (0 = the starting point).
 * @param thresholdDays Clean days required to reach this level.
 * @param title         Human-readable name shown in the UI.
 */
data class Level(
    val level: Int,
    val thresholdDays: Int,
    val title: String
) {
    /** The clean time, in millis, required to reach this level. */
    val thresholdMillis: Long get() = TimeUnit.DAYS.toMillis(thresholdDays.toLong())
}

/**
 * The milestone ladder. The day thresholds are user-customizable (see
 * [RecoveryState.levelThresholds] / Settings), while the level titles are fixed.
 *
 * [configure] must be called once on startup with the persisted thresholds; until
 * then the defaults are used. All level lookups go through the active config so the
 * whole app stays consistent.
 */
object Levels {

    /** Fixed, poetic titles for levels 0..5. */
    private val titles = listOf(
        "Beginning",
        "First Light",
        "Momentum",
        "One Week Strong",
        "Steadfast",
        "Transformed"
    )

    /** Default day thresholds for levels 0..5. */
    val defaultThresholds = listOf(0, 1, 3, 7, 14, 30)

    @Volatile
    private var thresholds: List<Int> = defaultThresholds

    /** Updates the active thresholds (called from the repository on load/change). */
    fun configure(newThresholds: List<Int>) {
        thresholds = sanitize(newThresholds)
    }

    /**
     * Ensures thresholds are valid: correct size, level 0 == 0 days, and strictly
     * increasing. Falls back to defaults for any malformed input.
     */
    fun sanitize(input: List<Int>): List<Int> {
        if (input.size != defaultThresholds.size) return defaultThresholds
        val fixed = input.toMutableList()
        fixed[0] = 0
        for (i in 1 until fixed.size) {
            // Each level must be strictly greater than the previous (min +1 day).
            if (fixed[i] <= fixed[i - 1]) fixed[i] = fixed[i - 1] + 1
        }
        return fixed
    }

    val all: List<Level>
        get() = thresholds.mapIndexed { i, days -> Level(i, days, titles[i]) }

    val maxLevel: Int get() = all.last().level

    /** The level reached for a given clean-streak duration. */
    fun levelForMillis(streakMillis: Long): Int {
        var result = 0
        for (l in all) {
            if (streakMillis >= l.thresholdMillis) result = l.level else break
        }
        return result
    }

    fun byNumber(level: Int): Level = all.firstOrNull { it.level == level } ?: all.first()

    /** The next level above [level], or null if already at the top. */
    fun next(level: Int): Level? = all.firstOrNull { it.level == level + 1 }

    /**
     * Grace-period duration for a protected level: equal to the time it originally
     * took to reach that level (its day threshold). Level 0 has no grace.
     */
    fun graceMillisForLevel(level: Int): Long = byNumber(level).thresholdMillis
}
