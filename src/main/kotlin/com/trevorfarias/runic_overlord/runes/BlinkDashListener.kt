package com.trevorfarias.runic_overlord.runes

import com.trevorfarias.runic_overlord.RunicOverlord
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerAnimationEvent
import org.bukkit.util.Vector
import java.util.*

class BlinkstepListener : Listener {

    private val cooldowns = mutableMapOf<UUID, Long>()
    private val COOLDOWN_MS = 1500L

    @EventHandler
    fun onSwingWhileSneaking(event: PlayerAnimationEvent) {
        val player = event.player
        val now = System.currentTimeMillis()

        if (!player.isSneaking) return
        if ((cooldowns[player.uniqueId] ?: 0L) > now - COOLDOWN_MS) return

        val runes = RuneFactory.getRunesFromEquipment(player)
        val hasBlinkstep = runes.any { it.id == "blinkstep1" }

        if (!hasBlinkstep) return

        // Teleport logic
        val direction: Vector = player.location.direction.normalize().multiply(3.0)
        val targetLoc = player.location.clone().add(direction)
        val block = player.world.getBlockAt(targetLoc)
        val above = block.getRelative(0, 1, 0)

        if (!block.type.isSolid && !above.type.isSolid) {
            cooldowns[player.uniqueId] = now
            player.teleport(targetLoc)
            player.world.playSound(player.location, "minecraft:entity.enderman.teleport", 0.5f, 1.8f)
        }
    }
}
