package com.trevorfarias.runic_overlord.runes

import com.trevorfarias.runic_overlord.RunicOverlord
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.*

data class RuneDef(
    val id: String,
    val name: String,
    val tier: Int,
    val material: Material,
    val attribute: Attribute,
    val amount: Double,
    val slot: List<EquipmentSlot>,
    val loreText: String
)



object RuneFactory {
    private val runes = mutableMapOf<String, RuneSpec>()
    fun allIds(): Collection<String> = runes.keys


    fun getRuneSpecById(id: String): RuneSpec? = runes[id]



    fun getRuneSpecByDisplayName(name: String): RuneSpec? {
        return runes.values.firstOrNull { it.displayName == name }
    }

    fun getRuneSpecFromItem(item: ItemStack): RuneSpec? {
        val meta = item.itemMeta ?: return null
        val key = NamespacedKey(RunicOverlord.instance, "rune_id")
        val id = meta.persistentDataContainer.get(key, PersistentDataType.STRING) ?: return null
        return getRuneSpecById(id)
    }


    fun getRunesFromEquipment(player: Player): List<RuneSpec> {
        val equipment = player.equipment ?: return emptyList()

        val items = listOfNotNull(
            equipment.helmet,
            equipment.chestplate,
            equipment.leggings,
            equipment.boots
        )

        return items.mapNotNull { getRuneSpecFromItem(it) }
    }


    fun createRune(id: String): ItemStack? {
        val rune = runes[id] ?: return null
        val item = ItemStack(rune.material)
        val meta = item.itemMeta ?: return item

        meta.setDisplayName(rune.displayName)
        meta.lore = rune.lore
        meta.addEnchant(Enchantment.INFINITY, 1, true)
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)

        val key = NamespacedKey(RunicOverlord.instance, "rune_id")
        meta.persistentDataContainer.set(key, PersistentDataType.STRING, id)

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
        uuids: Map<EquipmentSlot, UUID>
    ) {
        runes[id] = RuneSpec(id, displayName, tier, material, modifiers, validSlots, lore, applyModifier, removeModifier, uuids)
    }

    private fun toRoman(tier: Int): String = when (tier) {
        1 -> "I"
        2 -> "II"
        3 -> "III"
        4 -> "IV"
        5 -> "V"
        else -> tier.toString()
    }


    private val predefinedRunes = listOf(
        RuneDef("swiftness1", "Swiftness", 1, Material.FEATHER, Attribute.MOVEMENT_SPEED, 0.015, listOf(EquipmentSlot.FEET), "§7Increases speed slightly"),
        RuneDef("swiftness2", "Swiftness", 2, Material.FEATHER, Attribute.MOVEMENT_SPEED, 0.015, listOf(EquipmentSlot.FEET), "§7Increases speed slightly"),
        RuneDef("swiftness3", "Swiftness", 3, Material.FEATHER, Attribute.MOVEMENT_SPEED, 0.015, listOf(EquipmentSlot.FEET), "§7Increases speed slightly"),
        RuneDef("vitality1", "Vitality", 1, Material.RESIN_CLUMP, Attribute.MAX_HEALTH, 2.0, listOf(EquipmentSlot.CHEST), "§7Increases health slightly"),
        RuneDef("vitality2", "Vitality", 2, Material.RESIN_CLUMP, Attribute.MAX_HEALTH, 2.0, listOf(EquipmentSlot.CHEST), "§7Increases health moderately"),
        RuneDef("vitality3", "Vitality", 3, Material.RESIN_CLUMP, Attribute.MAX_HEALTH, 2.0, listOf(EquipmentSlot.CHEST), "§7Increases health significantly"),
        RuneDef("luck1", "Luck", 1, Material.KELP, Attribute.LUCK, 1.5, listOf(EquipmentSlot.CHEST, EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.HEAD), "§7Increases luck slightly"),
        RuneDef("luck2", "Luck", 2, Material.KELP, Attribute.LUCK, 1.5, listOf(EquipmentSlot.CHEST, EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.HEAD), "§7Increases luck moderately"),
        RuneDef("luck3", "Luck", 3, Material.KELP, Attribute.LUCK, 1.5, listOf(EquipmentSlot.CHEST, EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.HEAD), "§7Increases luck significantly"),
        RuneDef("gravity1", "Gravity", 1, Material.KELP, Attribute.GRAVITY, 0.1, listOf(EquipmentSlot.CHEST, EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.HEAD), "§7Increases gravity slightly"),
        RuneDef("gravity2", "Gravity", 2, Material.KELP, Attribute.GRAVITY, 0.2, listOf(EquipmentSlot.CHEST, EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.HEAD), "§7Increases gravity moderately"),
        RuneDef("gravity3", "Gravity", 3, Material.KELP, Attribute.GRAVITY, 0.3, listOf(EquipmentSlot.CHEST, EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.HEAD), "§7Increases gravity significantly"),
        RuneDef("blinkstep1", "Blinkstep", 1, Material.CHORUS_FRUIT, Attribute.LUCK, 0.0, listOf(EquipmentSlot.FEET), "§7Shift + Right click to teleport 3 blocks forward"),
        RuneDef("blinkstep2", "Blinkstep", 2, Material.CHORUS_FRUIT, Attribute.LUCK, 0.0, listOf(EquipmentSlot.FEET), "§7Shift + Right click to teleport forward"),
        RuneDef("blinkstep3", "Blinkstep", 3, Material.CHORUS_FRUIT, Attribute.LUCK, 0.0, listOf(EquipmentSlot.FEET), "§7Shift + Right click to teleport forward"),

        )


    init {
        predefinedRunes.forEach { def ->
            val runeId = def.id
            val displayName = "§bRune of ${def.name} ${toRoman(def.tier)}"
            val lore = listOf(def.loreText)
            val uuids = def.slot.associateWith { slot ->
                UUID.nameUUIDFromBytes("$runeId-${slot.name}".toByteArray())
            }

            val modifiers = def.slot.map { slot ->
                RuneModifier(def.attribute, def.amount, AttributeModifier.Operation.ADD_NUMBER, slot)
            }


            register(
                id = runeId,
                displayName = displayName,
                tier = def.tier,
                material = def.material,
                modifiers = modifiers,
                lore = lore,
                uuids = uuids,
                validSlots = def.slot,
                applyModifier = { item ->
                    item.clone().apply {
                        val meta = itemMeta ?: return@apply
                        def.slot.forEach { slot ->
                            val uuid = uuids[slot]!!
                            meta.getAttributeModifiers(def.attribute)
                                ?.filter { it.uniqueId == uuid }
                                ?.forEach { meta.removeAttributeModifier(def.attribute, it) }

                            meta.addAttributeModifier(
                                def.attribute,
                                AttributeModifier(
                                    uuid,
                                    "rune_${runeId}_${slot.name}",
                                    def.amount * def.tier,
                                    AttributeModifier.Operation.ADD_NUMBER,
                                    slot
                                )
                            )
                        }
                        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                        itemMeta = meta
                    }
                },
                removeModifier = { item ->
                    val clone = item.clone()
                    val meta = clone.itemMeta ?: return@register clone

                    Bukkit.getLogger().info("🔍 [Rune Remove] BEGIN for $runeId")

                    def.slot.forEach { slot ->
                        val uuid = uuids[slot] ?: return@forEach
                        val allMods = meta.getAttributeModifiers(def.attribute) ?: return@forEach

                        // Print before
                        allMods.forEach {
                            Bukkit.getLogger().info("→ Before: ${it.name} (${it.uniqueId})")
                        }

                        // Remove all modifiers for that attribute
                        meta.removeAttributeModifier(def.attribute)

                        // Restore any unrelated modifiers (not ours)
                        allMods
                            .filterNot { it.uniqueId == uuid }
                            .forEach {
                                meta.addAttributeModifier(def.attribute, it)
                                Bukkit.getLogger().info("↩️ Preserved unrelated modifier: ${it.name}")
                            }

                        Bukkit.getLogger().info("✅ Removed rune modifier with UUID: $uuid")
                        val key = NamespacedKey(RunicOverlord.instance, "rune_id")
                        val currentId = meta.persistentDataContainer.get(key, PersistentDataType.STRING)
                        if (currentId == runeId) {
                            meta.persistentDataContainer.remove(key)
                        }
                    }

                    meta.removeItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES)
                    clone.itemMeta = meta

                    // Final confirmation
                    val finalMods = meta.attributeModifiers
                    if (finalMods == null || finalMods.isEmpty()) {
                        Bukkit.getLogger().info("✅ Post-removal: No modifiers remain")
                    } else {
                        finalMods.forEach { attr, mod ->
                            Bukkit.getLogger().info("📌 Post-removal: $attr → ${mod.name} (${mod.uniqueId})")
                        }

                    }

                    return@register clone
                }


            )
        }
    }

}
