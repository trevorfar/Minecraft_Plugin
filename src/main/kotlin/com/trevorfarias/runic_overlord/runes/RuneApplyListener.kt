package com.trevorfarias.runic_overlord.runes

import com.trevorfarias.runic_overlord.RunicOverlord
import com.trevorfarias.runic_overlord.util.Constants
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

class RuneApplyListener : Listener {

    object PendingVoucherUse {
        val map: MutableMap<Player, ItemStack> = mutableMapOf()
    }

    object PendingRuneRemovals {
        val map = mutableMapOf<Player, ItemStack>()
    }

    @EventHandler
    fun onRuneApply(e: InventoryClickEvent) {
        val player = e.whoClicked as? Player ?: return
        if (player.gameMode != GameMode.SURVIVAL) return
        if (e.click != ClickType.LEFT) return
        if (e.clickedInventory != player.inventory) return

        val cursor = e.cursor // item being held
        var armorItem = e.currentItem ?: return
        if (armorItem.type.isAir || cursor.type.isAir) return



        // Handle applying a rune
        val runeSpec = RuneFactory.getRuneSpecFromItem(cursor) ?: return

        val runeId = runeSpec.id
        val runeIdBase = runeId.replace(Regex("\\d+\$"), "")

        if (!armorItem.type.name.contains("HELMET") &&
            !armorItem.type.name.contains("CHESTPLATE") &&
            !armorItem.type.name.contains("LEGGINGS") &&
            !armorItem.type.name.contains("BOOTS")
        ) return

        val equipmentSlot = when {
            armorItem.type.name.endsWith("_HELMET") -> EquipmentSlot.HEAD
            armorItem.type.name.endsWith("_CHESTPLATE") -> EquipmentSlot.CHEST
            armorItem.type.name.endsWith("_LEGGINGS") -> EquipmentSlot.LEGS
            armorItem.type.name.endsWith("_BOOTS") -> EquipmentSlot.FEET
            else -> null
        }



        if (equipmentSlot == null || equipmentSlot !in runeSpec.validSlots) {
            player.sendMessage("§cThis rune cannot be applied to that armor piece.")
            return
        }


        val meta = armorItem.itemMeta ?: return
        val pdc = meta.persistentDataContainer

        val runeList = pdc.get(Constants.RUNE_IDS_KEY, PersistentDataType.STRING)
            ?.split(",")?.filter { it.isNotBlank() }?.toMutableList() ?: mutableListOf()

        val existingRuneId = runeList.find {
            it.replace(Regex("\\d+\$"), "") == runeIdBase
        }

        if (existingRuneId != null) {
            val existingSpec = RuneFactory.getRuneSpecById(existingRuneId)
            val existingTier = existingSpec?.tier ?: 0

            if (existingTier >= runeSpec.tier) {
                player.sendMessage("§eA rune of equal or higher tier is already applied.")
                e.isCancelled = true
                return
            }

            // Remove existing rune's modifiers and ID
            armorItem = existingSpec?.removeModifier?.invoke(armorItem) ?: armorItem
            runeList.remove(existingRuneId)
        } else if (runeList.size >= Constants.MAX_RUNES) {
            player.sendMessage("§cThis armor has no empty rune slots.")
            e.isCancelled = true
            return
        }

        // Apply new rune
        val modifiedArmor = armorItem.clone()
        val modifiedMeta = modifiedArmor.itemMeta ?: return
        val oldLore = modifiedMeta.lore ?: emptyList()

        if (runeSpec.id !in runeList) {
            runeList.add(runeSpec.id)
        }

        val cleanedLore = oldLore.filterNot {
            it.startsWith("§bApplied: ") ||
                    it.startsWith("§7Runes:") ||
                    it.matches(Regex("§[0-9a-f]• Rune of .*")) ||
                    it.matches(Regex("§[0-9a-f]◆+")) ||
                    it.matches(Regex("§[0-9a-fA-F]• .*"))

        }.toMutableList()



        val runeData = runeList
            .distinct()
            .mapNotNull { id ->
                val spec = RuneFactory.getRuneSpecById(id) ?: return@mapNotNull null
                val symbol = when (spec.tier) {
                    1 -> "§f◆" to "§f"
                    2 -> "§b◆" to "§b"
                    3 -> "§3◆" to "§3"
                    else -> "§7◆" to "§7"
                }
                Triple(symbol.first, symbol.second, spec.displayName)
            }

        val visualBar = runeData.joinToString("") { it.first } + "§8" + "◇".repeat(Constants.MAX_RUNES - runeData.size)
        cleanedLore.add("§7Runes: $visualBar")
        val uniqueDisplayLore = mutableSetOf<String>()

        runeData.forEach { (_, color, name) ->
            if (uniqueDisplayLore.add(name)) {
                cleanedLore.add("$color• $name")
            }
        }
        modifiedMeta.lore = cleanedLore

        val updatedArmor = runeSpec.applyModifier(modifiedArmor)
        val updatedMeta = updatedArmor.itemMeta ?: return

        updatedMeta.lore = cleanedLore
        updatedMeta.persistentDataContainer.set(Constants.RUNE_SLOT_KEY, PersistentDataType.INTEGER, runeList.size)
        updatedMeta.persistentDataContainer.set(Constants.RUNE_IDS_KEY, PersistentDataType.STRING, runeList.joinToString(","))
        updatedMeta.persistentDataContainer.set(
            NamespacedKey(RunicOverlord.instance, "rune_id"),
            PersistentDataType.STRING,
            runeSpec.id
        )


        updatedArmor.itemMeta = updatedMeta
        e.currentItem = updatedArmor

        cursor.amount -= 1
        player.sendMessage("§aApplied ${runeSpec.displayName} to your armor.")
        e.isCancelled = true
    }

}
