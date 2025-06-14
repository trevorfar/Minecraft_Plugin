package com.trevorfarias.runic_overlord.runes

import com.trevorfarias.runic_overlord.RunicOverlord
import com.trevorfarias.runic_overlord.runes.RuneApplyListener.PendingRuneRemovals
import com.trevorfarias.runic_overlord.runes.RuneApplyListener.PendingVoucherUse
import com.trevorfarias.runic_overlord.util.Constants
import com.trevorfarias.runic_overlord.voucher.RuneGUI.openRuneRemovalGUI
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

        val pending = PendingRuneRemovals.map.remove(player) ?: return
        var armor   = pending.item

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

        val cleanedLore = updatedMeta.lore
            ?.filterNot {
                it.startsWith("§7Runes:") ||
                        it.matches(Regex("§[0-9a-fA-F]• .*")) ||
                        it.matches(Regex("§[0-9a-f]◆+"))
            }?.toMutableList() ?: mutableListOf()

        cleanedLore.add("§7Runes: $visualBar")

        runeList.distinct()
            .mapNotNull { RuneFactory.getRuneSpecById(it) }
            .forEach { spec ->
                val color = when (spec.tier) {
                    1 -> "§f"; 2 -> "§b"; 3 -> "§3"; else -> "§7"
                }
                cleanedLore.add("$color• ${spec.displayName}")
            }

        updatedMeta.lore = cleanedLore

        updatedMeta.persistentDataContainer.set(Constants.RUNE_IDS_KEY, PersistentDataType.STRING, runeList.joinToString(","))
        updatedMeta.persistentDataContainer.set(Constants.RUNE_SLOT_KEY, PersistentDataType.INTEGER, runeList.size)
        armor.itemMeta = updatedMeta

        val eq = player.equipment!!
        if (pending.equipped) {
            /* it was already worn – replace the right armour slot */
            when (pending.slot) {                // Bukkit indices
                39 -> eq.helmet     = armor      // HEAD
                38 -> eq.chestplate = armor      // CHEST
                37 -> eq.leggings   = armor      // LEGS
                36 -> eq.boots      = armor      // FEET
            }
        } else {
            /* it came from the main inventory – put it back there */
            player.inventory.setItem(pending.slot, armor)
        }
        player.updateInventory()
        completedRemovals.add(player.uniqueId)                      // don’t refund voucher
        player.closeInventory()                                     // close the “Remove a Rune” GUI
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

    fun handleRuneDrag(
        player : Player,
        armor  : ItemStack,
        voucher: ItemStack,
        e      : InventoryClickEvent
    ): Boolean {

        /* ── 1. check the item actually has runes ─────────────────────────── */
        val meta = armor.itemMeta ?: return false
        val runeIds = meta.persistentDataContainer
            .get(Constants.RUNE_IDS_KEY, PersistentDataType.STRING)
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?: emptyList()

        if (runeIds.isEmpty()) {
            player.sendMessage("§cThat item has no runes to remove.")
            return true
        }

        /* ── 2. remember voucher + armour + where it sat (slot & equipped) ─ */
        PendingVoucherUse.map[player] = voucher

        val equipped = e.slot in 36..39                // 39-HEAD, 38-CHEST, 37-LEGS, 36-FEET
        PendingRuneRemovals.map[player] = RuneApplyListener.PendingRemoval(
            item      = armor,
            slot      = e.slot,
            equipped  = equipped
        )

        /* ── 3. clear cursor & open the removal GUI ───────────────────────── */
        e.setCursor(ItemStack(Material.AIR))
        e.isCancelled = true
        openRuneRemovalGUI(player, armor, runeIds)

        return true
    }
}
