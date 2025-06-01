package com.trevorfarias.runic_overlord.runes

import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.util.Vector

class BlinkstepListener : Listener {

    @EventHandler
    fun onShiftRightClick(event: PlayerInteractEvent) {
        val player = event.player

        if (!player.isSneaking || !event.action.name.contains("RIGHT")) return

        val equippedRunes = RuneFactory.getRunesFromEquipment(player)
        Bukkit.getLogger().info("$equippedRunes")
        val hasBlinkstep = equippedRunes.any { it.id == "blinkstep1" }

        if (!hasBlinkstep) return

        val direction: Vector = player.location.direction.normalize().multiply(3.0)
        val targetLoc = player.location.clone().add(direction)

        val block = player.world.getBlockAt(targetLoc)
        val above = block.getRelative(0, 1, 0)

        if (!block.type.isSolid && !above.type.isSolid) {
            player.teleport(targetLoc)
            player.world.playSound(player.location, "minecraft:entity.enderman.teleport", 0.5f, 1.8f)
        }
    }
}
