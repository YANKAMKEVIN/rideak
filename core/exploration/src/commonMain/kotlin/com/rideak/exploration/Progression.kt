package com.rideak.exploration

/**
 * Everything progression is computed from. Derived from stored rides like the rest (nothing to keep
 * in sync), so changing a rule replays the whole history. Speed is deliberately absent: RIDEAK
 * rewards exploring, never riding fast.
 */
data class PlayerStats(
    val rides: Int = 0,
    val distanceMeters: Double = 0.0,
    val exploredCells: Int = 0,
    val discoveredPlaces: Map<PlaceCategory, Int> = emptyMap(),
    /** Regions (cities) with at least one explored rideable cell. */
    val exploredRegions: Int = 0,
) {
    val discoveredPlaceCount: Int get() = discoveredPlaces.values.sum()
}

data class Level(
    val number: Int,
    val title: String,
    val xp: Int,
    /** XP at which this level started and the next one starts. */
    val levelStartXp: Int,
    val nextLevelXp: Int,
) {
    val fractionToNext: Float get() = (xp - levelStartXp).toFloat() / (nextLevelXp - levelStartXp)
}

object Progression {
    const val XP_PER_CELL = 10
    const val XP_PER_PLACE = 50
    const val XP_PER_RIDE = 20
    const val XP_PER_KM = 5

    private val titles = listOf(
        "Novice", "Éclaireur", "Aventurier", "Pionnier", "Cartographe",
        "Explorateur", "Grand explorateur", "Héros du terrain", "Légende",
    )

    fun xp(stats: PlayerStats): Int =
        stats.exploredCells * XP_PER_CELL +
            stats.discoveredPlaceCount * XP_PER_PLACE +
            stats.rides * XP_PER_RIDE +
            (stats.distanceMeters / 1000).toInt() * XP_PER_KM

    /** Level n starts at 100 × n(n−1)/2 XP: 0, 100, 300, 600, 1 000… each level a bit longer. */
    fun level(xp: Int): Level {
        var n = 1
        while (startXp(n + 1) <= xp) n++
        return Level(n, titles[(n - 1).coerceAtMost(titles.lastIndex)], xp, startXp(n), startXp(n + 1))
    }

    private fun startXp(level: Int) = 100 * level * (level - 1) / 2
}

/** Achievements, one per illustrated badge. Progress is always derived from [PlayerStats]. */
enum class Badge(val title: String, val description: String, val goal: Int) {
    FIRST_RIDE("1re sortie", "Termine ta première sortie", 1),
    TEN_KM("10 km", "Roule 10 km au total", 10),
    FIRST_PARK("Premier parc", "Découvre un parc", 1),
    FIVE_CITIES("5 villes", "Explore 5 communes", 5),
    HUNDRED_KM("100 km", "Roule 100 km au total", 100),
    EXPLORER("Explorateur", "Explore 100 zones", 100),
    ;

    fun progress(stats: PlayerStats): Int = when (this) {
        FIRST_RIDE -> stats.rides
        TEN_KM, HUNDRED_KM -> (stats.distanceMeters / 1000).toInt()
        FIRST_PARK -> stats.discoveredPlaces[PlaceCategory.PARK] ?: 0
        FIVE_CITIES -> stats.exploredRegions
        EXPLORER -> stats.exploredCells
    }.coerceAtMost(goal)

    fun unlocked(stats: PlayerStats): Boolean = progress(stats) >= goal

    companion object {
        /** Badges [after] has that [before] did not: the "BADGE DÉBLOQUÉ !" of a ride. */
        fun newlyUnlocked(before: PlayerStats, after: PlayerStats): List<Badge> =
            entries.filter { it.unlocked(after) && !it.unlocked(before) }
    }
}
