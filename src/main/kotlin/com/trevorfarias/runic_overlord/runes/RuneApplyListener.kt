package com.trevorfarias.runic_overlord.runes

import com.trevorfarias.runic_overlord.util.Constants
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

class RuneApplyListener : Listener {

    @EventHandler
    fun onRuneApply(e: InventoryClickEvent) {
        val player = e.whoClicked as? Player ?: return
        if (player.gameMode != GameMode.SURVIVAL) return
        if (e.click != ClickType.LEFT) return
        if (e.clickedInventory != player.inventory) return

        val cursor = e.cursor // item being held
        var armorItem = e.currentItem ?: return
        if (armorItem.type.isAir || cursor.type.isAir) return

        val cursorMeta = cursor.itemMeta ?: return

        // Handle rune remover tool
        if (cursorMeta.persistentDataContainer.has(Constants.IS_RUNE_REMOVER, PersistentDataType.BYTE)) {
            val armorMeta = armorItem.itemMeta ?: return
            val pdc = armorMeta.persistentDataContainer
            val runeIds = pdc.get(Constants.RUNE_IDS_KEY, PersistentDataType.STRING)
                ?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

            if (runeIds.isEmpty()) {
                player.sendMessage("§eThis armor has no runes.")
                return
            }

            openRuneRemovalGUI(player, armorItem.clone(), runeIds)
            e.isCancelled = true
            return
        }

        // Handle applying a rune
        val runeSpec = RuneFactory.getRuneSpecFromItem(cursor) ?: return
        val runeId = runeSpec.id
        val runeIdBase = runeId.replace(Regex("\\d+\$"), "")

        if (!armorItem.type.name.contains("HELMET") &&
            !armorItem.type.name.contains("CHESTPLATE") &&
            !armorItem.type.name.contains("LEGGINGS") &&
            !armorItem.type.name.contains("BOOTS")) return

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

        runeList.add(runeSpec.id)

        val cleanedLore = oldLore.filterNot {
            it.startsWith("§bApplied: ") || it.startsWith("§7Runes:")
        }.toMutableList()

        val runeSymbols = runeList.mapNotNull { id ->
            val spec = RuneFactory.getRuneSpecById(id) ?: return@mapNotNull null
            when (spec.tier) {
                1 -> "§f◆"
                2 -> "§b◆"
                3 -> "§3◆"
                else -> "§7◆"
            }
        }
        val visualBar = runeSymbols.joinToString("") + "§8" + "◇".repeat(Constants.MAX_RUNES - runeSymbols.size)
        cleanedLore.add("§7Runes: $visualBar")
        modifiedMeta.lore = cleanedLore

        val updatedArmor = runeSpec.applyModifier(modifiedArmor)
        val updatedMeta = updatedArmor.itemMeta ?: return

        updatedMeta.lore = cleanedLore
        updatedMeta.persistentDataContainer.set(Constants.RUNE_SLOT_KEY, PersistentDataType.INTEGER, runeList.size)
        updatedMeta.persistentDataContainer.set(Constants.RUNE_IDS_KEY, PersistentDataType.STRING, runeList.joinToString(","))

        updatedArmor.itemMeta = updatedMeta
        e.currentItem = updatedArmor

        cursor.amount -= 1
        player.sendMessage("§aApplied ${runeSpec.displayName} to your armor.")
        e.isCancelled = true
    }

    private fun openRuneRemovalGUI(player: Player, armor: ItemStack, runeIds: List<String>) {
        val size = ((runeIds.size + 8) / 9) * 9 // Round up to nearest multiple of 9
        val gui = Bukkit.createInventory(null, size, "§cRemove a Rune")

        runeIds.forEachIndexed { i, id ->
            val spec = RuneFactory.getRuneSpecById(id) ?: return@forEachIndexed
            val item = ItemStack(spec.material)
            val meta = item.itemMeta ?: return@forEachIndexed
            meta.setDisplayName("§cRemove: ${spec.displayName}")
            item.itemMeta = meta
            gui.setItem(i, item)
        }

        PendingRuneRemovals.map[player] = armor
        player.openInventory(gui)
    }
}
