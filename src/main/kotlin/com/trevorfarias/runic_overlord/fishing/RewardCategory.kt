package com.trevorfarias.runic_overlord.fishing

/**
 * Maps Fishing-Level → a named category.
 *
 * `unlocked(level)` returns *all* categories ≤ player level, so higher tiers
 * keep getting prior rewards.
 */
enum class RewardCategory(val pretty: String, val min: Int, val max: Int) {
    JOURNEYMAN ("Journeyman", 1,  10),
    APPRENTICE ("Apprentice",  10, 25),
    VETERAN    ("Veteran",     25, 50),
    EXPERT     ("Expert",      50, 75),
    GRANDMASTER("Grand Master",75, 100);

    companion object {
        /** Highest category the player has reached */
        fun forLevel(lvl: Int) = values().last { lvl >= it.min }

        /** All categories the player can roll from */
        fun unlocked(lvl: Int) = values().filter { lvl >= it.min }
    }
}
