package com.trevorfarias.runic_overlord.runes

import com.trevorfarias.runic_overlord.util.Constants
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.persistence.PersistentDataType

object RuneRemoverGuiListener : Listener {

    @EventHandler
    fun onRuneRemovalSelection(event: InventoryClickEvent) {
        val player = event.whoClicked as? org.bukkit.entity.Player ?: return
        if (event.view.title != "§cRemove a Rune") return
        event.isCancelled = true

        val clickedItem = event.currentItem ?: return
        val clickedName = clickedItem.itemMeta?.displayName ?: return
        if (!clickedName.startsWith("§cRemove: ")) return

        val runeDisplayName = clickedName.removePrefix("§cRemove: ")
        val targetRune = RuneFactory.getRuneSpecByDisplayName(runeDisplayName) ?: return

        var armor = PendingRuneRemovals.map.remove(player) ?: return
        val meta = armor.itemMeta ?: return
        val pdc = meta.persistentDataContainer

        // Get current rune list
        val runeList = pdc.get(Constants.RUNE_IDS_KEY, PersistentDataType.STRING)
            ?.split(",")?.filter { it.isNotBlank() }?.toMutableList() ?: return

        // Remove target rune ID
        if (!runeList.remove(targetRune.id)) {
            player.sendMessage("§cThat rune is no longer applied.")
            player.closeInventory()
            return
        }

        // Remove attribute modifiers from item
        armor = targetRune.removeModifier(armor)
        val updatedMeta = armor.itemMeta ?: return

        // Regenerate rune bar
        val visualBar = runeList.mapNotNull {
            val spec = RuneFactory.getRuneSpecById(it) ?: return@mapNotNull null
            when (spec.tier) {
                1 -> "§f◆"
                2 -> "§b◆"
                3 -> "§3◆"
                else -> "§7◆"
            }
        }.joinToString("") + "§8" + "◇".repeat(Constants.MAX_RUNES - runeList.size)

        val cleanedLore = updatedMeta.lore?.filterNot { it.startsWith("§7Runes:") }?.toMutableList() ?: mutableListOf()
        cleanedLore.add("§7Runes: $visualBar")
        updatedMeta.lore = cleanedLore

        // Update rune metadata
        updatedMeta.persistentDataContainer.set(Constants.RUNE_IDS_KEY, PersistentDataType.STRING, runeList.joinToString(","))
        updatedMeta.persistentDataContainer.set(Constants.RUNE_SLOT_KEY, PersistentDataType.INTEGER, runeList.size)
        armor.itemMeta = updatedMeta

        val equipment = player.equipment ?: return
        targetRune.modifiers.map { it.slot }.distinct().forEach { slot ->
            when (slot) {
                EquipmentSlot.HEAD -> equipment.helmet = armor
                EquipmentSlot.CHEST -> equipment.chestplate = armor
                EquipmentSlot.LEGS -> equipment.leggings = armor
                EquipmentSlot.FEET -> equipment.boots = armor
                else -> {} // ignore unsupported slots
            }
        }



        player.sendMessage("§aRemoved ${targetRune.displayName} from your armor.")
        player.closeInventory()
    }
}
