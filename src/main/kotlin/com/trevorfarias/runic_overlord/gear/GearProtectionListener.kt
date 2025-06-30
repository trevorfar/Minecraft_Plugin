package com.trevorfarias.runic_overlord.gear

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockDispenseEvent
import org.bukkit.event.block.BlockPlaceEvent

/**
 * Cancels every block-place attempt if the item being placed
 * is one of your custom GearSpecs.
 */
object GearProtectionListener : Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockPlace(e: BlockPlaceEvent) {

        val item = e.itemInHand ?: return                      // nothing in hand
        if (GearFactory.getSpec(item) != null) {               // custom gear?
            e.isCancelled = true                               // block won’t appear
            e.player.sendMessage("§cThat’s a tool, not a block!")
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onDispense(e: BlockDispenseEvent) {
        if (GearFactory.getSpec(e.item) != null) e.isCancelled = true
    }
}
