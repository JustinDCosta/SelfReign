package com.aldrenstudios.selfreign.data

/** The category of outcome produced when a relapse is processed. */
enum class RelapseOutcome {
    /** First-ever relapse: forgiven once, nothing lost. Show the warning screen. */
    FIRST_FORGIVENESS,

    /** A fresh grace period has begun; protected items remain temporarily accessible. */
    GRACE_STARTED,

    /** Relapsed during an active grace (or it had expired): items hard-locked. */
    HARD_LOCK
}

/** Result of processing a relapse: the new state plus a description of what happened. */
data class RelapseResult(
    val newState: RecoveryState,
    val outcome: RelapseOutcome,
    val protectedLevel: Int,
    val graceEndTimestamp: Long
)

/**
 * Pure, deterministic relapse state machine. Has no Android dependencies so it can
 * be unit-tested in isolation.
 *
 * The rules (in strict priority order):
 *
 * 1. FIRST-TIME FORGIVENESS
 *    If [RecoveryState.hasUsedFirstForgiveness] is false, this is the user's first
 *    relapse. Forgive it: the streak is NOT reset, no level/items are lost, and the
 *    flag is set so it can never trigger again.
 *
 * 2. GRACE PERIOD (subsequent relapse, not already in grace)
 *    The user keeps temporary access to the items of the level they had reached.
 *    A grace timer is opened whose duration equals the time it originally took to
 *    reach that level (its day threshold). The streak resets to now, so the user
 *    must organically climb back to [protectedLevel] before the timer expires.
 *
 * 3. HARD LOCK
 *    If the user relapses again while a grace period is still active, OR (handled in
 *    [evaluateExpiry]) the grace timer expires before they regain their level, the
 *    protected items are revoked. The streak resets and there is no shield.
 */
object RelapseEngine {

    /**
     * Upper bound on retained relapse-log entries. Keeps on-device storage and backup
     * file size bounded over years of use; the most recent entries are always kept.
     */
    const val MAX_LOG_ENTRIES = 500

    /**
     * Processes a relapse at [now] and returns the resulting state + outcome.
     * An optional [note] and [trigger] tag are stored with the log entry.
     */
    fun processRelapse(
        state: RecoveryState,
        now: Long,
        note: String? = null,
        trigger: String? = null
    ): RelapseResult {
        // The streak that is ending (used to update the best-ever streak on resets).
        val endingStreak = state.streakMillis(now)
        val newBest = maxOf(state.bestStreakMillis, endingStreak)
        val cleanNote = note?.trim()?.takeIf { it.isNotEmpty() }

        // Append a log entry, capping total history to MAX_LOG_ENTRIES (keeping the most
        // recent) so storage, the encrypted blob, and backup files stay bounded over the
        // app's lifetime. The History UI shows the most recent entries regardless.
        fun appendLog(outcome: RelapseOutcome): List<RelapseLogEntry> =
            (state.relapseLog + RelapseLogEntry(now, outcome.name, cleanNote, trigger))
                .takeLast(MAX_LOG_ENTRIES)

        // RULE 1: First-time forgiveness. Streak is preserved entirely.
        if (!state.hasUsedFirstForgiveness) {
            val newState = state.copy(
                hasUsedFirstForgiveness = true,
                relapseCount = state.relapseCount + 1,
                // Streak is preserved, so best-streak keeps growing from it later;
                // record the current value so the stat is never lower than reality.
                bestStreakMillis = newBest,
                relapseLog = appendLog(RelapseOutcome.FIRST_FORGIVENESS)
                // streakStartTimestamp intentionally unchanged - fully forgiven.
            )
            return RelapseResult(newState, RelapseOutcome.FIRST_FORGIVENESS, state.organicLevel(now), 0L)
        }

        val graceCurrentlyActive = state.graceActive && now < state.graceEndTimestamp

        // RULE 3: Already inside an active grace period -> hard lock everything.
        if (graceCurrentlyActive) {
            val newState = state.copy(
                relapseCount = state.relapseCount + 1,
                bestStreakMillis = newBest,
                streakStartTimestamp = now,
                graceActive = false,
                graceStartTimestamp = 0L,
                graceEndTimestamp = 0L,
                graceProtectedLevel = 0,
                relapseLog = appendLog(RelapseOutcome.HARD_LOCK)
            )
            return RelapseResult(newState, RelapseOutcome.HARD_LOCK, 0, 0L)
        }

        // RULE 2: Open a grace period protecting the level the user had reached.
        val protectedLevel = state.organicLevel(now)
        if (protectedLevel <= 0) {
            // Nothing meaningful to protect; behaves like a clean reset (hard lock at 0).
            val newState = state.copy(
                relapseCount = state.relapseCount + 1,
                bestStreakMillis = newBest,
                streakStartTimestamp = now,
                graceActive = false,
                graceStartTimestamp = 0L,
                graceEndTimestamp = 0L,
                graceProtectedLevel = 0,
                relapseLog = appendLog(RelapseOutcome.HARD_LOCK)
            )
            return RelapseResult(newState, RelapseOutcome.HARD_LOCK, 0, 0L)
        }

        val graceDuration = Levels.graceMillisForLevel(protectedLevel)
        val graceEnd = now + graceDuration
        val newState = state.copy(
            relapseCount = state.relapseCount + 1,
            bestStreakMillis = newBest,
            streakStartTimestamp = now,
            graceActive = true,
            graceStartTimestamp = now,
            graceEndTimestamp = graceEnd,
            graceProtectedLevel = protectedLevel,
            relapseLog = appendLog(RelapseOutcome.GRACE_STARTED)
        )
        return RelapseResult(newState, RelapseOutcome.GRACE_STARTED, protectedLevel, graceEnd)
    }

    /**
     * Resolves a grace period that may have ended. Called opportunistically (on app
     * open / tick). Two ways grace closes successfully or fails:
     *
     *  - SUCCESS: the user's organic level has reached the protected level again ->
     *    grace is cleared cleanly; items are now genuinely earned.
     *  - EXPIRY (HARD LOCK): the timer elapsed before they regained the level ->
     *    grace is cleared and protection is dropped (items revoke to organic level).
     *
     * Returns the possibly-updated state. If nothing changed, returns [state] unchanged.
     */
    fun evaluateExpiry(state: RecoveryState, now: Long): RecoveryState {
        if (!state.graceActive) return state

        // User climbed back to (or above) the protected level: grace satisfied.
        if (state.organicLevel(now) >= state.graceProtectedLevel) {
            return state.copy(
                graceActive = false,
                graceStartTimestamp = 0L,
                graceEndTimestamp = 0L,
                graceProtectedLevel = 0
            )
        }

        // Timer expired without regaining the level: hard lock.
        if (now >= state.graceEndTimestamp) {
            return state.copy(
                graceActive = false,
                graceStartTimestamp = 0L,
                graceEndTimestamp = 0L,
                graceProtectedLevel = 0
            )
        }

        // Grace still running.
        return state
    }
}
