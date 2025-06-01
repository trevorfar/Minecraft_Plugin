package com.trevorfarias.runic_overlord.runes

import com.trevorfarias.runic_overlord.runes.RuneFactory
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent

class RuneProtectionListener : Listener {

    @EventHandler
    fun onPlaceRune(event: BlockPlaceEvent) {
        val item = event.itemInHand ?: return
        val rune = RuneFactory.getRuneSpecFromItem(item) ?: return
        event.isCancelled = true
    }

    @EventHandler
    fun onRightClickRune(event: PlayerInteractEvent) {
        val item = event.item ?: return
        val rune = RuneFactory.getRuneSpecFromItem(item) ?: return

        val materialName = item.type.name
        val isArmor = materialName.contains("HELMET") ||
                materialName.contains("CHESTPLATE") ||
                materialName.contains("LEGGINGS") ||
                materialName.contains("BOOTS")

        if (!isArmor && event.action.toString().contains("RIGHT_CLICK")) {
            event.isCancelled = true
        }
    }
}
