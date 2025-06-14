package com.trevorfarias.runic_overlord.gear

import net.kyori.adventure.text.format.NamedTextColor.*

/** Static helper that maps a roll (1-100) to the game’s Rarity enum. */
object QualityScale {

    private val ranges = mapOf(
        Rarity.COMMON     to  1..40,
        Rarity.UNCOMMON   to 41..60,
        Rarity.RARE       to 61..80,
        Rarity.EPIC       to 81..90,
        Rarity.LEGENDARY  to 91..100
    )

    fun rarityOf(percent: Int): Rarity =
        ranges.entries.first { percent in it.value }.key

    /** Colour codes taken from your old enum. */
    fun Rarity.colour() = when (this) {
        Rarity.COMMON    -> GRAY
        Rarity.UNCOMMON  -> GREEN
        Rarity.RARE      -> AQUA
        Rarity.EPIC      -> LIGHT_PURPLE
        Rarity.LEGENDARY -> GOLD
    }
}

enum class Rarity(val weight: Int) {
    COMMON(60),
    UNCOMMON(30),
    RARE(7),
    EPIC(2),
    LEGENDARY(1)
}

enum class Tier(val weight: Int) {
    I(80),
    II(20),
    III(5),
}
