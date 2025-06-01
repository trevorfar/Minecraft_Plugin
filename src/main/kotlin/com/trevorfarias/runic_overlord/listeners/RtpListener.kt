package com.trevorfarias.runic_overlord.listeners

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerPortalEvent
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector

class RtpListener : Listener {
    private val portalWorld = "spawn"       // world where the portal is
    private val portalX = 0.5                // center x-coord of portal
    private val portalZ = 0.3                // center z-coord of portal
    private val radius = 1.0                 // how close player has to be
    private val targetWorld = "TownyWorld"  // world to RTP into

    @EventHandler
    fun onPortal(event: PlayerPortalEvent) {
        val player = event.player
        val loc = player.location

        if (loc.world?.name != portalWorld) return
        if (loc.distance(loc.world!!.spawnLocation.set(loc.x, loc.y, loc.z)) > radius) return

        // cancel original teleport
        event.isCancelled = true

        // run BetterRTP command
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "rtp world $targetWorld ${player.name}")
    }
}