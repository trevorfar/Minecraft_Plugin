package com.trevorfarias.runic_overlord

import com.trevorfarias.runic_overlord.commands.CustomItemCommand
import com.trevorfarias.runic_overlord.listeners.SandBreakListener
import com.trevorfarias.runic_overlord.runes.*
import com.trevorfarias.runic_overlord.voucher.VoucherUseListener
import org.bukkit.Bukkit
import org.bukkit.event.Listener

object PluginManager {
    private val registeredListeners = mutableListOf<Listener>()

    fun initialize(plugin: RunicOverlord) {
        plugin.logger.info("Initializing Runic Overlord systems...")

        register(plugin, SandBreakListener())
        register(plugin, VoucherUseListener())
        register(plugin, RuneApplyListener())
        register(plugin, RuneEffectListener())
        register(plugin, RuneRemoverGuiListener)
        register(plugin, RuneProtectionListener())
        register(plugin, RuneLootInjector)
        register(plugin, BlinkstepListener())

        plugin.getCommand("givecustom")?.setExecutor(CustomItemCommand())
    }

    fun shutdown(plugin: RunicOverlord) {
        plugin.logger.info("Shutting down Runic Overlord systems...")
        Bukkit.getScheduler().cancelTasks(plugin)

        // Unregister all event listeners
        val handlerList = org.bukkit.event.HandlerList.getHandlerLists()
        handlerList.forEach { list ->
            registeredListeners.forEach {
                list.unregister(it)
            }
        }

        registeredListeners.clear()
    }

    private fun register(plugin: RunicOverlord, listener: Listener) {
        plugin.server.pluginManager.registerEvents(listener, plugin)
        registeredListeners.add(listener)
    }
}
