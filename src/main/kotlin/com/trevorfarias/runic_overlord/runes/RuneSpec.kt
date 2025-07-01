package com.trevorfarias.runic_overlord.runes

import org.bukkit.Material
import org.bukkit.attribute.AttributeModifier
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.attribute.Attribute
import java.util.*

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
    val validSlots: List<EquipmentSlot> = listOf(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET), // default: all
    val lore: List<String>,
    val applyModifier: (ItemStack) -> ItemStack,
    val removeModifier: (ItemStack) -> ItemStack,
    val uuids: Map<EquipmentSlot, UUID>

)