/* src/main/kotlin/com/trevorfarias/runic_overlord/gear/GearSpec.kt */
package com.trevorfarias.runic_overlord.gear

import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.inventory.EquipmentSlot
import java.util.*

data class GearSpec(
    val id: String,
    val material: Material,
    val minDamage: Double,
    val maxDamage: Double,
    val displayName: String,
    val tierLore: (Int) -> List<String>,              // lore builder, receives quality %
    val slots: Set<EquipmentSlot> = setOf(EquipmentSlot.HAND)
) {
    /** Attribute modifiers are calculated *after* quality is known */
    fun buildAttrMods(qualityPct: Int): Collection<AttributeModifier> {
        val dmg = minDamage + (maxDamage - minDamage) * (qualityPct / 100.0)
        return listOf(
            AttributeModifier(
                UUID.nameUUIDFromBytes("$id-damage".toByteArray()),
                "gear_$id_damage",
                dmg,                                       // absolute attackDamage value
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.HAND
            )
        )
    }
}
