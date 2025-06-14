/* src/main/kotlin/com/trevorfarias/runic_overlord/gear/GearSpec.kt */
package com.trevorfarias.runic_overlord.gear

import org.bukkit.Material
import org.bukkit.inventory.EquipmentSlot

/* GearSpec.kt */
data class GearSpec(
    val id: String,
    val material: Material,
    val displayName: String,
    val stats: List<StatModifier>,
    val loreBuilder: (Int, Rarity) -> List<String>,
    val equipmentSlots: Set<EquipmentSlot> = setOf(EquipmentSlot.HAND),
    val locationTags: List<String> = emptyList(),
    val rarity: Rarity = Rarity.COMMON,
    val abilityBaseChance: Double = 0.0,
    val tier: Tier = Tier.I)


