package com.aldrenstudios.selfreign.data

/** Kind of unlockable store item. */
enum class StoreItemType { WALLPAPER, MUSIC }

/**
 * A single unlockable store item.
 *
 * @param id              Stable identifier (persisted; never change once shipped).
 * @param type            Wallpaper or music.
 * @param displayName     Shown in the store.
 * @param requiredLevel   Level at which this item unlocks.
 * @param rawResName      For MUSIC items: the expected res/raw file name (without
 *                        extension). Resolved at runtime so the app still compiles
 *                        and runs even if the audio asset has not been added yet.
 */
data class StoreItem(
    val id: String,
    val type: StoreItemType,
    val displayName: String,
    val requiredLevel: Int,
    val rawResName: String? = null
) {
    /** The clean-day requirement, derived from the unlocking level. */
    val requiredDays: Int get() = Levels.byNumber(requiredLevel).thresholdDays
}

/**
 * The fixed catalog of everything that can be unlocked. Wallpapers are rendered
 * as code-defined gradients (see ui/theme/Wallpapers.kt), so they ship with no
 * binary assets. Music items reference optional res/raw files.
 */
object StoreCatalog {

    val items: List<StoreItem> = listOf(
        // Wallpapers
        StoreItem("wp_black", StoreItemType.WALLPAPER, "Pure Black", requiredLevel = 0),
        StoreItem("wp_sage", StoreItemType.WALLPAPER, "Sage Mist", requiredLevel = 1),
        StoreItem("wp_ocean", StoreItemType.WALLPAPER, "Deep Ocean", requiredLevel = 2),
        StoreItem("wp_lavender", StoreItemType.WALLPAPER, "Lavender Dusk", requiredLevel = 3),
        StoreItem("wp_aurora", StoreItemType.WALLPAPER, "Aurora", requiredLevel = 4),
        StoreItem("wp_cosmos", StoreItemType.WALLPAPER, "Cosmos", requiredLevel = 5),

        // Ambient music tracks (optional res/raw assets resolved by name)
        StoreItem("mus_rain", StoreItemType.MUSIC, "Gentle Rain", requiredLevel = 2, rawResName = "ambient_rain"),
        StoreItem("mus_forest", StoreItemType.MUSIC, "Forest Dawn", requiredLevel = 3, rawResName = "ambient_forest"),
        StoreItem("mus_waves", StoreItemType.MUSIC, "Ocean Waves", requiredLevel = 4, rawResName = "ambient_waves"),
        StoreItem("mus_drone", StoreItemType.MUSIC, "Calm Drone", requiredLevel = 5, rawResName = "ambient_drone")
    )

    fun byId(id: String): StoreItem? = items.firstOrNull { it.id == id }

    val wallpapers: List<StoreItem> get() = items.filter { it.type == StoreItemType.WALLPAPER }
    val music: List<StoreItem> get() = items.filter { it.type == StoreItemType.MUSIC }

    /** All items that unlock at or below [level]. */
    fun unlockedAt(level: Int): List<StoreItem> = items.filter { it.requiredLevel <= level }
}
