package com.trevorfarias.runic_overlord.identifier

import com.trevorfarias.runic_overlord.gear.GearFactory
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack

class IdentifierUseListener : Listener {

    @EventHandler
    fun onItemIdentify(e: InventoryClickEvent) {
        val player = e.whoClicked as? Player ?: return
        if (e.click != ClickType.LEFT) return
        if (e.clickedInventory != player.inventory) return      // only player inv

        val cursor = e.cursor
        if (!IdentifierFactory.isIdentifier(cursor)) return     // not holding an ID

        val target = e.currentItem ?: return
        val spec   = GearFactory.getSpec(target) ?: return      // not custom gear
        val qual   = GearFactory.getQuality(target) ?: return
        if (qual >= 0) {                                        // already revealed
            player.sendMessage("§7That item is already identified.")
            return
        }

        // ---------- reveal ----------------------------------------------------
        e.isCancelled = true
        GearFactory.markIdentified(target, GearFactory.rollQuality())
        player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f)

        // consume one identifier sheet
        if (cursor.amount <= 1) {
            e.setCursor(ItemStack(Material.AIR))                // clear cursor
        } else {
            cursor.amount = cursor.amount - 1                   // or cursor.amount--
        }

        player.updateInventory()
    }
}
