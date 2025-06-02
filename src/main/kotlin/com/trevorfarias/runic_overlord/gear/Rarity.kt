/* com/trevorfarias/runic_overlord/gear/Rarity.kt */
package com.trevorfarias.runic_overlord.gear

import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.NamedTextColor.*

enum class Rarity(
    val range: IntRange,
    val display: String,
    val colour: TextColor,
    val weight: Double                // rollQuality() uses this
) {
    POOR       ( 1..40,  "Poor",      RED,        0.25),
    COMMON    (41..60,  "Common",    GRAY,       0.35),
    UNCOMMON  (61..80,  "Uncommon",  GREEN,      0.25),
    RARE      (81..90,  "Rare",      AQUA,       0.10),
    EPIC      (91..97,  "Epic",      LIGHT_PURPLE,0.04),
    LEGENDARY (98..100, "Legendary", GOLD,       0.01);

    companion object {
        fun of(percent: Int) = values().first { percent in it.range }
    }
}
