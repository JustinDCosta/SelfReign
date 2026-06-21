package com.aldrenstudios.selfreign

import com.aldrenstudios.selfreign.data.Levels
import com.aldrenstudios.selfreign.data.RecoveryState
import com.aldrenstudios.selfreign.data.RelapseEngine
import com.aldrenstudios.selfreign.data.RelapseOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Exercises the deterministic relapse/grace/forgiveness state machine.
 * Times are expressed relative to a fixed "now" for clarity.
 */
class RelapseEngineTest {

    private val now = 1_000_000_000_000L
    private fun daysAgo(d: Int) = now - TimeUnit.DAYS.toMillis(d.toLong())

    @Test
    fun firstRelapse_isForgiven_streakPreserved() {
        // 8 clean days -> Level 3, first ever relapse.
        val state = RecoveryState(
            streakStartTimestamp = daysAgo(8),
            hasUsedFirstForgiveness = false
        )
        val result = RelapseEngine.processRelapse(state, now)

        assertEquals(RelapseOutcome.FIRST_FORGIVENESS, result.outcome)
        assertTrue(result.newState.hasUsedFirstForgiveness)
        // Streak untouched -> still the same start, still Level 3.
        assertEquals(state.streakStartTimestamp, result.newState.streakStartTimestamp)
        assertEquals(3, result.newState.organicLevel(now))
        assertEquals(1, result.newState.relapseCount)
    }

    @Test
    fun secondRelapse_startsGrace_protectingReachedLevel() {
        // Forgiveness already used; 14 clean days -> Level 4.
        val state = RecoveryState(
            streakStartTimestamp = daysAgo(14),
            hasUsedFirstForgiveness = true
        )
        val result = RelapseEngine.processRelapse(state, now)

        assertEquals(RelapseOutcome.GRACE_STARTED, result.outcome)
        assertEquals(4, result.protectedLevel)
        assertTrue(result.newState.graceActive)
        // Grace duration == time to reach Level 4 == 14 days.
        val expectedEnd = now + Levels.graceMillisForLevel(4)
        assertEquals(expectedEnd, result.newState.graceEndTimestamp)
        // Streak resets to now.
        assertEquals(now, result.newState.streakStartTimestamp)
        // Items for level 4 remain accessible via the shield.
        assertEquals(4, result.newState.effectiveLevel(now))
    }

    @Test
    fun relapseDuringGrace_hardLocks() {
        // Already inside an active grace protecting level 4.
        val state = RecoveryState(
            streakStartTimestamp = now,
            hasUsedFirstForgiveness = true,
            graceActive = true,
            graceStartTimestamp = now,
            graceEndTimestamp = now + TimeUnit.DAYS.toMillis(14),
            graceProtectedLevel = 4
        )
        val result = RelapseEngine.processRelapse(state, now + 1000)

        assertEquals(RelapseOutcome.HARD_LOCK, result.outcome)
        assertFalse(result.newState.graceActive)
        assertEquals(0, result.newState.graceProtectedLevel)
        assertEquals(0, result.newState.effectiveLevel(now + 1000))
    }

    @Test
    fun graceExpires_withoutRegainingLevel_hardLocks() {
        val state = RecoveryState(
            streakStartTimestamp = now,
            hasUsedFirstForgiveness = true,
            graceActive = true,
            graceStartTimestamp = now,
            graceEndTimestamp = now + TimeUnit.DAYS.toMillis(14),
            graceProtectedLevel = 4
        )
        // Evaluate 15 days later: timer expired, organic level still 0.
        val after = state.copy() // streak still starts at `now`
        val resolved = RelapseEngine.evaluateExpiry(after, now + TimeUnit.DAYS.toMillis(15))

        assertFalse(resolved.graceActive)
        assertEquals(0, resolved.graceProtectedLevel)
    }

    @Test
    fun graceSatisfied_whenLevelRegained_beforeExpiry() {
        // Grace protecting level 4, started 1 day ago, streak also 1 day in... but
        // to regain level 4 the user needs 14 organic days. Simulate that by setting
        // streakStart far enough back so organic level >= 4 while timer still runs.
        val graceStart = daysAgo(1)
        val state = RecoveryState(
            streakStartTimestamp = daysAgo(14), // organic level already 4 again
            hasUsedFirstForgiveness = true,
            graceActive = true,
            graceStartTimestamp = graceStart,
            graceEndTimestamp = graceStart + TimeUnit.DAYS.toMillis(14), // still active
            graceProtectedLevel = 4
        )
        val resolved = RelapseEngine.evaluateExpiry(state, now)

        assertFalse("grace cleared on success", resolved.graceActive)
        assertEquals(0, resolved.graceProtectedLevel)
        // Level is genuinely earned now.
        assertEquals(4, resolved.organicLevel(now))
    }

    @Test
    fun relapseAtLevelZero_hardLocksImmediately() {
        // Forgiveness used, but only a few hours clean -> level 0, nothing to protect.
        val state = RecoveryState(
            streakStartTimestamp = now - TimeUnit.HOURS.toMillis(3),
            hasUsedFirstForgiveness = true
        )
        val result = RelapseEngine.processRelapse(state, now)

        assertEquals(RelapseOutcome.HARD_LOCK, result.outcome)
        assertFalse(result.newState.graceActive)
    }

    @Test
    fun everyRelapse_isRecordedInLog_withBestStreakTracked() {
        // 14 clean days, forgiveness already used -> grace, and the 14-day streak
        // should be captured as the best streak.
        val state = RecoveryState(
            streakStartTimestamp = daysAgo(14),
            hasUsedFirstForgiveness = true
        )
        val result = RelapseEngine.processRelapse(state, now)

        assertEquals(1, result.newState.relapseLog.size)
        assertEquals(RelapseOutcome.GRACE_STARTED.name, result.newState.relapseLog.first().outcome)
        assertEquals(now, result.newState.relapseLog.first().timestamp)
        // Best streak >= the 14 days that just ended.
        assertTrue(result.newState.bestStreakMillis >= TimeUnit.DAYS.toMillis(14))
    }

    @Test
    fun relapse_recordsNoteAndTrigger() {
        val state = RecoveryState(streakStartTimestamp = daysAgo(14), hasUsedFirstForgiveness = true)
        val result = RelapseEngine.processRelapse(state, now, note = "  stressful day  ", trigger = "STRESS")

        val entry = result.newState.relapseLog.first()
        // Note is trimmed; trigger is stored verbatim.
        assertEquals("stressful day", entry.note)
        assertEquals("STRESS", entry.trigger)
    }

    @Test
    fun blankNote_isStoredAsNull() {
        val state = RecoveryState(streakStartTimestamp = daysAgo(14), hasUsedFirstForgiveness = true)
        val result = RelapseEngine.processRelapse(state, now, note = "   ", trigger = null)
        assertEquals(null, result.newState.relapseLog.first().note)
    }

    @Test
    fun relapseLog_isCappedAtMaximum() {
        // Seed a log already at the cap, then process one more relapse.
        val seeded = (1..RelapseEngine.MAX_LOG_ENTRIES).map {
            com.aldrenstudios.selfreign.data.RelapseLogEntry(it.toLong(), RelapseOutcome.HARD_LOCK.name)
        }
        val state = RecoveryState(
            streakStartTimestamp = daysAgo(14),
            hasUsedFirstForgiveness = true,
            relapseLog = seeded
        )
        val result = RelapseEngine.processRelapse(state, now)

        // Size stays capped and the newest entry is retained (oldest dropped).
        assertEquals(RelapseEngine.MAX_LOG_ENTRIES, result.newState.relapseLog.size)
        assertEquals(now, result.newState.relapseLog.last().timestamp)
    }
}
