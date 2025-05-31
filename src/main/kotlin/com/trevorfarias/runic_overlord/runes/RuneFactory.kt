package com.trevorfarias.runic_overlord.runes

import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import java.util.*

object RuneFactory {
    private val runes = mutableMapOf<String, RuneSpec>()

    fun getRuneSpecById(id: String): RuneSpec? = runes[id]

    fun getRuneSpecFromItem(item: ItemStack): RuneSpec? {
        val meta = item.itemMeta ?: return null
        return runes.values.firstOrNull {
            it.displayName == meta.displayName && it.lore == meta.lore && item.type == it.material
        }
    }

    fun getRuneSpecByDisplayName(name: String): RuneSpec? {
        return runes.values.firstOrNull { it.displayName == name }
    }

    fun getRuneIdFromItem(item: ItemStack): String? {
        val meta = item.itemMeta ?: return null
        return runes.entries.firstOrNull {
            val spec = it.value
            spec.displayName == meta.displayName && spec.lore == meta.lore && item.type == spec.material
        }?.key
    }

    fun createRune(id: String): ItemStack? {
        val rune = runes[id] ?: return null
        val item = ItemStack(rune.material)
        val meta = item.itemMeta ?: return item
        meta.setDisplayName(rune.displayName)
        meta.lore = rune.lore
        meta.addEnchant(org.bukkit.enchantments.Enchantment.INFINITY, 1, true)
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)
        item.itemMeta = meta
        return item
    }

    private fun register(
        id: String,
        displayName: String,
        tier: Int,
        material: Material,
        modifiers: List<RuneModifier>,
        lore: List<String>,
        applyModifier: (ItemStack) -> ItemStack,
        removeModifier: (ItemStack) -> ItemStack,
        validSlots: List<EquipmentSlot> = listOf(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET), // default: all
    ) {
        runes[id] = RuneSpec(id, displayName, tier, material, modifiers, validSlots, lore, applyModifier, removeModifier)
    }


    init {
        register(
            id = "swiftness1",
            displayName = "§bRune of Swiftness I",
            tier = 1,
            material = Material.FEATHER,
            modifiers = listOf(
                RuneModifier(
                    attribute = Attribute.MOVEMENT_SPEED,
                    amount = 0.015, // base amount per tier
                    operation = AttributeModifier.Operation.ADD_NUMBER,
                    slot = EquipmentSlot.FEET
                )
            ),
            validSlots = listOf(EquipmentSlot.FEET),
            lore = listOf("§7Increases speed slightly"),
            applyModifier = applier(1, "swiftness1"),
            removeModifier = remover("swiftness1")
        )

        register(
            id = "swiftness2",
            displayName = "§bRune of Swiftness II",
            tier = 2,
            material = Material.FEATHER,
            modifiers = listOf(
                RuneModifier(
                    attribute = Attribute.MOVEMENT_SPEED,
                    amount = 0.015, // base amount per tier
                    operation = AttributeModifier.Operation.ADD_NUMBER,
                    slot = EquipmentSlot.FEET
                )
            ),
            validSlots = listOf(EquipmentSlot.FEET),
            lore = listOf("§7Increases speed slightly"),
            applyModifier = applier(2, "swiftness2"),
            removeModifier = remover("swiftness2")
        )

        register(
            id = "swiftness3",
            displayName = "§bRune of Swiftness III",
            tier = 3,
            material = Material.FEATHER,
            modifiers = listOf(
                RuneModifier(
                    attribute = Attribute.MOVEMENT_SPEED,
                    amount = 0.015, // base amount per tier
                    operation = AttributeModifier.Operation.ADD_NUMBER,
                    slot = EquipmentSlot.FEET
                )
            ),
            validSlots = listOf(EquipmentSlot.FEET),
            lore = listOf("§7Increases speed slightly"),
            applyModifier = applier(3, "swiftness3"),
            removeModifier = remover("swiftness3")
        )

    }

    private fun applier(tier: Int, id: String): (ItemStack) -> ItemStack = { armor ->
        armor.clone().apply {
            val meta = itemMeta ?: return@apply
            val uuid = UUID.nameUUIDFromBytes(id.toByteArray())
            val runeSpec = getRuneSpecById(id) ?: return@apply

            runeSpec.modifiers.forEach { mod ->
                meta.removeAttributeModifier(mod.attribute)
            }
            runeSpec.modifiers.forEach { baseMod ->
                val scaledAmount = baseMod.amount * tier
                val newModifier = AttributeModifier(
                    uuid,
                    "${runeSpec.id}$tier",
                    scaledAmount,
                    baseMod.operation,
                    baseMod.slot
                )
                meta.addAttributeModifier(baseMod.attribute, newModifier)
            }
            itemMeta = meta
        }
    }

    private fun remover(id: String): (ItemStack) -> ItemStack = { armor ->
        armor.clone().apply {
            val meta = itemMeta ?: return@apply
            val uuid = UUID.nameUUIDFromBytes(id.toByteArray())
            val runeSpec = getRuneSpecById(id) ?: return@apply

            runeSpec.modifiers.forEach { mod ->
                val existing = meta.getAttributeModifiers(mod.attribute)
                existing?.filter { it.uniqueId == uuid }?.forEach {
                    meta.removeAttributeModifier(mod.attribute, it)
                }
            }
            itemMeta = meta
        }
    }




}
