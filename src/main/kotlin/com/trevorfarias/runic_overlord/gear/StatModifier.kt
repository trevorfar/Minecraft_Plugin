/* com/trevorfarias/runic_overlord/gear/StatModifier.kt */
package com.trevorfarias.runic_overlord.gear

import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.meta.ItemMeta
import java.util.*

sealed interface StatModifier {
    fun apply(item: ItemMeta, qualityPct: Int)
    fun remove(item: ItemMeta)

}


data class AttributeStat(
    val attribute: Attribute,
    val slot: EquipmentSlot,
    val min: Double,
    val max: Double,
    val op: AttributeModifier.Operation = AttributeModifier.Operation.ADD_NUMBER,
    val uuidSeed: String = UUID.randomUUID().toString()
) : StatModifier {
    override fun remove(item: ItemMeta) {
        item.removeAttributeModifier(attribute)
    }
    override fun apply(item: ItemMeta, qualityPct: Int) {
        val value = min + (max - min) * (qualityPct / 100.0)
        val modifier = AttributeModifier(
            UUID.nameUUIDFromBytes("$uuidSeed-$qualityPct".toByteArray()),
            "gear_${attribute.name().lowercase()}",
            value,
            op,
            slot
        )
        item.addAttributeModifier(attribute, modifier)
    }
}

data class EnchantStat(
    val enchant: org.bukkit.enchantments.Enchantment,
    val minLvl: Int,
    val maxLvl: Int
) : StatModifier {
    override fun remove(item: ItemMeta) {
        item.removeEnchant(enchant)
    }
    override fun apply(item: org.bukkit.inventory.meta.ItemMeta, qualityPct: Int) {
        val lvl = (minLvl + (maxLvl - minLvl) * (qualityPct / 100.0)).toInt().coerceAtLeast(1)
        item.addEnchant(enchant, lvl, true)
    }
}

data class UnbreakableStat(
    val alwaysTrue: Boolean = true         // <- satisfies the rule
) : StatModifier {
    override fun remove(item: ItemMeta) {
        item.isUnbreakable = false
    }
    override fun apply(item: ItemMeta, qualityPct: Int) {
        item.isUnbreakable = true
    }
}

/* you can keep adding new StatModifier implementations—e.g., PotionEffectStat, ModelOverrideStat, etc. */
