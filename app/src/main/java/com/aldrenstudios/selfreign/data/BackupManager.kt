package com.aldrenstudios.selfreign.data

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Serializes and deserializes [RecoveryState] to/from a structured JSON document.
 *
 * The format is versioned and every field is validated on import with defensive
 * checks, so a malformed, truncated, or tampered file produces a clean
 * [ImportResult.Error] instead of crashing the app.
 */
object BackupManager {

    private const val BACKUP_VERSION = 3
    private const val MIN_SUPPORTED_VERSION = 1
    private const val MAGIC = "reclaim.backup"

    /** Outcome of an import attempt. */
    sealed interface ImportResult {
        data class Success(val state: RecoveryState) : ImportResult
        data class Error(val reason: String) : ImportResult
    }

    /** Produces a pretty-printed JSON string representing [state]. */
    fun export(state: RecoveryState): String {
        val root = JSONObject()
        root.put("magic", MAGIC)
        root.put("version", BACKUP_VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        val s = JSONObject().apply {
            put("streakStartTimestamp", state.streakStartTimestamp)
            put("relapseCount", state.relapseCount)
            put("hasUsedFirstForgiveness", state.hasUsedFirstForgiveness)
            put("bestStreakMillis", state.bestStreakMillis)
            put("relapseLog", RelapseLogCodec.toJsonArray(state.relapseLog))
            put("graceActive", state.graceActive)
            put("graceStartTimestamp", state.graceStartTimestamp)
            put("graceEndTimestamp", state.graceEndTimestamp)
            put("graceProtectedLevel", state.graceProtectedLevel)
            put("onboardingComplete", state.onboardingComplete)
            put("selectedWallpaperId", state.selectedWallpaperId)
            put("hapticsEnabled", state.hapticsEnabled)
            put("levelThresholds", JSONArray(state.levelThresholds))
            put("costPerDayCents", state.costPerDayCents)
            put("currencySymbol", state.currencySymbol)
            // SECURITY: the app lock is intentionally treated as device-local. We never
            // write the PIN hash (or lock flags) into a portable, plaintext backup file,
            // because a short PIN's hash would be trivially brute-forced offline. These
            // are deliberately exported as inert defaults and ignored on import.
            put("appLockEnabled", false)
            put("biometricEnabled", false)
            put("pinHash", JSONObject.NULL)
        }
        root.put("state", s)
        return root.toString(2)
    }

    /**
     * Parses and validates [raw]. Never throws; all failures become
     * [ImportResult.Error]. Defends against missing keys, wrong magic/version,
     * out-of-range numbers, and unknown references.
     */
    fun import(raw: String): ImportResult {
        if (raw.isBlank()) return ImportResult.Error("File is empty.")
        if (raw.length > 1_000_000) return ImportResult.Error("File is too large to be a valid backup.")

        val root = try {
            JSONObject(raw)
        } catch (e: JSONException) {
            return ImportResult.Error("Not a valid JSON backup file.")
        }

        if (root.optString("magic") != MAGIC) {
            return ImportResult.Error("Unrecognized file. This is not a Reclaim backup.")
        }
        val version = root.optInt("version", -1)
        if (version < MIN_SUPPORTED_VERSION || version > BACKUP_VERSION) {
            return ImportResult.Error("Unsupported backup version: $version.")
        }

        val s = root.optJSONObject("state")
            ?: return ImportResult.Error("Backup is missing its state section.")

        return try {
            val now = System.currentTimeMillis()

            val streakStart = s.getLong("streakStartTimestamp")
            // A streak start far in the future is invalid (clock tamper / corruption).
            if (streakStart > now + ONE_DAY_MS) {
                return ImportResult.Error("Backup contains an invalid (future) start time.")
            }

            val relapseCount = s.getInt("relapseCount").coerceAtLeast(0)
            val graceLevel = s.getInt("graceProtectedLevel").coerceIn(0, Levels.maxLevel)

            // Wallpaper id must exist in the catalog; otherwise fall back to default.
            val wallpaperId = s.optString("selectedWallpaperId", "wp_black")
                .let { if (StoreCatalog.byId(it) != null) it else "wp_black" }

            val state = RecoveryState(
                streakStartTimestamp = streakStart,
                relapseCount = relapseCount,
                hasUsedFirstForgiveness = s.getBoolean("hasUsedFirstForgiveness"),
                bestStreakMillis = s.optLong("bestStreakMillis", 0L).coerceAtLeast(0L),
                relapseLog = RelapseLogCodec.fromJsonArray(s.optJSONArray("relapseLog")),
                graceActive = s.getBoolean("graceActive"),
                graceStartTimestamp = s.getLong("graceStartTimestamp").coerceAtLeast(0),
                graceEndTimestamp = s.getLong("graceEndTimestamp").coerceAtLeast(0),
                graceProtectedLevel = graceLevel,
                onboardingComplete = s.getBoolean("onboardingComplete"),
                selectedWallpaperId = wallpaperId,
                hapticsEnabled = s.optBoolean("hapticsEnabled", false),
                levelThresholds = Levels.sanitize(
                    s.optJSONArray("levelThresholds")?.let { arr ->
                        (0 until arr.length()).map { arr.optInt(it) }
                    } ?: Levels.defaultThresholds
                ),
                costPerDayCents = s.optInt("costPerDayCents", 0).coerceAtLeast(0),
                currencySymbol = s.optString("currencySymbol", "$").ifBlank { "$" }
                // SECURITY: app-lock fields (appLockEnabled / biometricEnabled / pinHash)
                // are deliberately NOT restored from a backup. They are device-local and
                // are preserved from the importing device by the caller (see
                // MainViewModel.importJson), so a restore can never lock a user out or
                // import a foreign credential.
            )
            ImportResult.Success(state)
        } catch (e: JSONException) {
            ImportResult.Error("Backup is missing required fields or has the wrong format.")
        } catch (e: Exception) {
            ImportResult.Error("Could not read backup: ${e.message ?: "unknown error"}.")
        }
    }

    private const val ONE_DAY_MS = 24L * 60 * 60 * 1000
}
