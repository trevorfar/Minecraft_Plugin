package com.trevorfarias.runic_overlord.runes

import com.trevorfarias.runic_overlord.RunicOverlord
import com.trevorfarias.runic_overlord.runes.RuneApplyListener.PendingRuneRemovals
import com.trevorfarias.runic_overlord.runes.RuneApplyListener.PendingVoucherUse
import com.trevorfarias.runic_overlord.util.Constants
import com.trevorfarias.runic_overlord.voucher.VoucherFactory
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.*


object RuneRemoverGuiListener : Listener {

    private val completedRemovals = mutableSetOf<UUID>()

    @EventHandler
    fun onInventoryDragWithVoucher(e: InventoryClickEvent) {
        val player = e.whoClicked as? Player ?: return
        val cursor = e.cursor ?: return
        val target = e.currentItem ?: return
        val voucher = VoucherFactory.matchVoucher(cursor) ?: return

        voucher.customAction?.let { action ->
            if (action(player, target, cursor, e)) {
                e.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onRuneRemovalSelection(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
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

        val runeList = pdc.get(Constants.RUNE_IDS_KEY, PersistentDataType.STRING)
            ?.split(",")?.filter { it.isNotBlank() }?.toMutableList() ?: return

        if (!runeList.remove(targetRune.id)) {
            player.sendMessage("§cThat rune is no longer applied.")
            return
        }

        armor = targetRune.removeModifier(armor)
        val updatedMeta = armor.itemMeta ?: return

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
                else -> {}
            }
        }

        completedRemovals.add(player.uniqueId)
        player.closeInventory()
        player.sendMessage("§aRemoved ${targetRune.displayName} from your armor.")
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (event.view.title == "§cRemove a Rune") {
            val player = event.player as? Player ?: return
            val uuid = player.uniqueId

            if (!completedRemovals.remove(uuid)) {
                PendingVoucherUse.map.remove(player)?.let { voucher ->
                    player.inventory.addItem(voucher)
                }
            }

            PendingRuneRemovals.map.remove(player)
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        PendingVoucherUse.map.remove(event.player)?.let { voucher ->
            event.player.inventory.addItem(voucher)
        }
        PendingRuneRemovals.map.remove(event.player)
        completedRemovals.remove(event.player.uniqueId)
    }
}
