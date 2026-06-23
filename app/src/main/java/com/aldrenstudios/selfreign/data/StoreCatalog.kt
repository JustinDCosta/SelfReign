package com.aldrenstudios.selfreign.data

/**
 * A single unlockable store item (an app wallpaper).
 *
 * @param id              Stable identifier (persisted; never change once shipped).
 * @param displayName     Shown in the store.
 * @param requiredLevel   Level at which this item unlocks.
 */
data class StoreItem(
    val id: String,
    val displayName: String,
    val requiredLevel: Int
) {
    /** The clean-day requirement, derived from the unlocking level. */
    val requiredDays: Int get() = Levels.byNumber(requiredLevel).thresholdDays
}

/**
 * The fixed catalog of unlockable wallpapers. Wallpapers are rendered as
 * code-defined gradients (see ui/theme/Wallpapers.kt), so they ship with no
 * binary assets.
 */
object StoreCatalog {

    val items: List<StoreItem> = listOf(
        StoreItem("wp_black", "Pure Black", requiredLevel = 0),
        StoreItem("wp_sage", "Sage Mist", requiredLevel = 1),
        StoreItem("wp_ocean", "Deep Ocean", requiredLevel = 2),
        StoreItem("wp_lavender", "Lavender Dusk", requiredLevel = 3),
        StoreItem("wp_aurora", "Aurora", requiredLevel = 4),
        StoreItem("wp_cosmos", "Cosmos", requiredLevel = 5)
    )

    fun byId(id: String): StoreItem? = items.firstOrNull { it.id == id }

    /** All wallpapers (kept as a named accessor for the store UI). */
    val wallpapers: List<StoreItem> get() = items

    /** All items that unlock at or below [level]. */
    fun unlockedAt(level: Int): List<StoreItem> = items.filter { it.requiredLevel <= level }
}
