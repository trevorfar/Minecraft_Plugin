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

        val lastUsed = cooldowns[player.uniqueId] ?: 0L
        val timeSinceLast = now - lastUsed

        if (timeSinceLast < COOLDOWN_MS) {
            val remainingMs = COOLDOWN_MS - timeSinceLast
            val remainingSec = remainingMs / 1000.0
            player.sendMessage("§cThis ability is still on cooldown. ${"%.1f".format(remainingSec)}s remaining.")
            return
        }

        val runes = RuneFactory.getRunesFromEquipment(player)

        val runeTier = runes.firstOrNull {
            it.id == "blinkstep3" || it.id == "blinkstep2" || it.id == "blinkstep1"
        }

        val distance = when (runeTier?.id) {
            "blinkstep3" -> 7.0
            "blinkstep2" -> 5.0
            "blinkstep1" -> 3.0
            else -> return
        }

        // Teleport logic
        val direction: Vector = player.location.direction.normalize().multiply(distance)
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
