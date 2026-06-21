package com.aldrenstudios.selfreign

import com.aldrenstudios.selfreign.data.BackupManager
import com.aldrenstudios.selfreign.data.RecoveryState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies backup serialization round-trips recovery data and, critically, that the
 * device-local app-lock credential is never written to or restored from a portable
 * backup file (security requirement).
 */
class BackupManagerTest {

    @Test
    fun export_roundTrips_recoveryData() {
        val original = RecoveryState(
            streakStartTimestamp = 1_700_000_000_000L,
            relapseCount = 5,
            hasUsedFirstForgiveness = true,
            bestStreakMillis = 123_456_789L,
            costPerDayCents = 1250,
            currencySymbol = "£"
        )
        val json = BackupManager.export(original)
        val result = BackupManager.import(json)

        assertTrue(result is BackupManager.ImportResult.Success)
        val restored = (result as BackupManager.ImportResult.Success).state
        assertEquals(original.streakStartTimestamp, restored.streakStartTimestamp)
        assertEquals(original.relapseCount, restored.relapseCount)
        assertEquals(original.bestStreakMillis, restored.bestStreakMillis)
        assertEquals(1250, restored.costPerDayCents)
        assertEquals("£", restored.currencySymbol)
    }

    @Test
    fun export_neverContainsPinHash() {
        val withPin = RecoveryState(
            appLockEnabled = true,
            biometricEnabled = true,
            pinHash = "deadbeef:cafebabe"
        )
        val json = BackupManager.export(withPin)
        // The raw backup text must not leak the credential.
        assertFalse(json.contains("deadbeef"))
        assertFalse(json.contains("cafebabe"))
    }

    @Test
    fun import_neverRestoresLockState() {
        // Even a hand-crafted backup claiming a lock must import as unlocked, with no PIN.
        val crafted = RecoveryState(
            appLockEnabled = true,
            biometricEnabled = true,
            pinHash = "deadbeef:cafebabe"
        )
        // Force the lock fields into the JSON to simulate a tampered file.
        val tampered = BackupManager.export(crafted)
            .replace("\"appLockEnabled\": false", "\"appLockEnabled\": true")
            .replace("\"pinHash\": null", "\"pinHash\": \"deadbeef:cafebabe\"")

        val result = BackupManager.import(tampered)
        assertTrue(result is BackupManager.ImportResult.Success)
        val restored = (result as BackupManager.ImportResult.Success).state
        assertFalse(restored.appLockEnabled)
        assertFalse(restored.biometricEnabled)
        assertNull(restored.pinHash)
    }

    @Test
    fun import_rejectsGarbage() {
        assertTrue(BackupManager.import("not json at all") is BackupManager.ImportResult.Error)
        assertTrue(BackupManager.import("") is BackupManager.ImportResult.Error)
        assertTrue(BackupManager.import("{\"foo\":1}") is BackupManager.ImportResult.Error)
    }
}
