package com.aldrenstudios.selfreign.data

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max

/**
 * A single recorded relapse, kept for the History view.
 *
 * @param timestamp Epoch millis when it was logged.
 * @param outcome   Name of the [RelapseOutcome] that was applied.
 * @param note      Optional free-text note about what triggered it.
 * @param trigger   Optional trigger tag name (see [Trigger]).
 */
data class RelapseLogEntry(
    val timestamp: Long,
    val outcome: String,
    val note: String? = null,
    val trigger: String? = null
)

/** Predefined trigger categories the user can tag a relapse with. */
enum class Trigger(val label: String) {
    STRESS("Stress"),
    BOREDOM("Boredom"),
    SOCIAL("Social"),
    HABIT("Routine"),
    EMOTION("Emotion"),
    CRAVING("Craving"),
    OTHER("Other");

    companion object {
        fun fromName(name: String?): Trigger? = entries.firstOrNull { it.name == name }
    }
}

/** JSON (de)serialization for the relapse log, shared by the store and backup. */
object RelapseLogCodec {

    fun toJsonArray(log: List<RelapseLogEntry>): JSONArray {
        val arr = JSONArray()
        log.forEach { e ->
            arr.put(
                JSONObject()
                    .put("t", e.timestamp)
                    .put("o", e.outcome)
                    .put("n", e.note ?: JSONObject.NULL)
                    .put("g", e.trigger ?: JSONObject.NULL)
            )
        }
        return arr
    }

    fun fromJsonArray(arr: JSONArray?): List<RelapseLogEntry> {
        if (arr == null) return emptyList()
        val out = ArrayList<RelapseLogEntry>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val t = o.optLong("t", 0L)
            if (t <= 0L) continue
            val note = if (o.isNull("n")) null else o.optString("n", "").takeIf { it.isNotBlank() }
            val trigger = if (o.isNull("g")) null else o.optString("g", "").takeIf { it.isNotBlank() }
            out.add(RelapseLogEntry(t, o.optString("o", RelapseOutcome.HARD_LOCK.name), note, trigger))
        }
        return out
    }
}

/**
 * The complete, persisted recovery state. This is the single source of truth for
 * the gamification state machine and is stored encrypted on-device.
 *
 * All timestamps are epoch millis.
 */
data class RecoveryState(
    // --- Streak / progression ---
    val streakStartTimestamp: Long = System.currentTimeMillis(),
    val relapseCount: Int = 0,
    val hasUsedFirstForgiveness: Boolean = false,
    val bestStreakMillis: Long = 0L,
    val relapseLog: List<RelapseLogEntry> = emptyList(),

    // --- Grace period (see RelapseEngine for the rules) ---
    val graceActive: Boolean = false,
    val graceStartTimestamp: Long = 0L,
    val graceEndTimestamp: Long = 0L,
    val graceProtectedLevel: Int = 0,

    // --- Flow flags ---
    val onboardingComplete: Boolean = false,

    // --- Customization (store selections + feedback toggles) ---
    val selectedWallpaperId: String = "wp_black",
    val selectedMusicId: String? = null,
    val musicEnabled: Boolean = false,
    val soundsEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,

    // --- Customizable milestones (days per level 0..5) ---
    val levelThresholds: List<Int> = Levels.defaultThresholds,

    // --- Money saved ("health/cost reclaimed") ---
    val costPerDayCents: Int = 0,        // user's estimated daily spend on the habit, in cents
    val currencySymbol: String = "$",

    // --- Security: app lock ---
    val appLockEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,  // allow biometric unlock in addition to PIN
    val pinHash: String? = null          // salted hash of the PIN
) {

    /** Clean-streak duration in millis at [now]. */
    fun streakMillis(now: Long): Long = (now - streakStartTimestamp).coerceAtLeast(0)

    /** The level earned purely from the current uninterrupted streak. */
    fun organicLevel(now: Long): Int = Levels.levelForMillis(streakMillis(now))

    /**
     * The level whose store items are currently *accessible*. During an active
     * grace period the protected level keeps its items unlocked as positive
     * reinforcement, even though the organic level may be lower.
     */
    fun effectiveLevel(now: Long): Int {
        val organic = organicLevel(now)
        return if (graceActive && now < graceEndTimestamp) max(organic, graceProtectedLevel) else organic
    }

    /** True when items are unlocked only because grace is shielding them. */
    fun isGraceShielding(now: Long): Boolean =
        graceActive && now < graceEndTimestamp && organicLevel(now) < graceProtectedLevel

    /** Remaining grace time in millis (0 when no active grace). */
    fun graceRemainingMillis(now: Long): Long =
        if (graceActive && now < graceEndTimestamp) graceEndTimestamp - now else 0L

    /** Whether a given store item is currently accessible at [now]. */
    fun isUnlocked(item: StoreItem, now: Long): Boolean = item.requiredLevel <= effectiveLevel(now)

    /** Longest streak ever, including the one currently in progress. */
    fun bestStreakIncludingCurrent(now: Long): Long = max(bestStreakMillis, streakMillis(now))

    /** Estimated money saved over the current streak, in cents. */
    fun moneySavedCents(now: Long): Long {
        if (costPerDayCents <= 0) return 0
        val days = streakMillis(now).toDouble() / 86_400_000.0
        return (days * costPerDayCents).toLong()
    }
}
