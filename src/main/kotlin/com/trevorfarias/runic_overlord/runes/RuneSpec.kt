package com.trevorfarias.runic_overlord.runes

import org.bukkit.Material
import org.bukkit.attribute.AttributeModifier
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.attribute.Attribute

data class RuneModifier(
    val attribute: Attribute,
    val amount: Double,
    val operation: AttributeModifier.Operation,
    val slot: EquipmentSlot
)

data class RuneSpec(
    val id: String,
    val displayName: String,
    val tier: Int = 1,
    val material: Material,
    val modifiers: List<RuneModifier>,
    val lore: List<String>,
    val applyModifier: (ItemStack) -> ItemStack,
    val removeModifier: (ItemStack) -> ItemStack
)