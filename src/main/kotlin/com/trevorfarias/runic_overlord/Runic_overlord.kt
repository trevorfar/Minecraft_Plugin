package com.trevorfarias.runic_overlord

import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.inventory.ItemStack
import kotlin.random.Random


class Runic_overlord : JavaPlugin(), Listener {

    override fun onEnable() {
        // Plugin startup logic
        getLogger().info("Hello world! Runic Overlord is up and ready");
        server.pluginManager.registerEvents(this, this)
    }

    override fun onDisable() {
        // Plugin shutdown logic
        getLogger().info("Disabled!");
    }

    @EventHandler
    fun onSandBreak(event: BlockBreakEvent) {
        if (event.block.type != Material.SAND) return

        val player = event.player

        if (Random.nextDouble() < 0.5) {
            player.inventory.addItem(ItemStack(Material.DIAMOND))
            player.sendMessage("💎 Lucky! You got a diamond.")
        }
    }
}
