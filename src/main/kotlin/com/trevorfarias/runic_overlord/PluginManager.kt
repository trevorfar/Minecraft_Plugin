package com.trevorfarias.runic_overlord

import com.trevorfarias.runic_overlord.commands.CustomItemCommand
import com.trevorfarias.runic_overlord.crafting.RemainsCraftingListener
import com.trevorfarias.runic_overlord.fishing.CustomFishingGearListener
import com.trevorfarias.runic_overlord.fishing.CustomFishingListener
import com.trevorfarias.runic_overlord.fishing.FishGuttingListener
import com.trevorfarias.runic_overlord.gear.CustomGearListener
import com.trevorfarias.runic_overlord.gear.GearProtectionListener
import com.trevorfarias.runic_overlord.identifier.IdentifierUseListener
import com.trevorfarias.runic_overlord.listeners.BarrierWand
import com.trevorfarias.runic_overlord.listeners.RtpListener
import com.trevorfarias.runic_overlord.listeners.SandBreakListener
import com.trevorfarias.runic_overlord.runes.*
import com.trevorfarias.runic_overlord.voucher.EnchantUpgradeVoucherListener
import com.trevorfarias.runic_overlord.voucher.QualityRerollVoucherListener
import com.trevorfarias.runic_overlord.voucher.VoucherActionListener
import com.trevorfarias.runic_overlord.voucher.VoucherUseListener
import org.bukkit.Bukkit
import org.bukkit.event.Listener

object PluginManager {
    private val registeredListeners = mutableListOf<Listener>()

    fun initialize(plugin: RunicOverlord) {
        plugin.logger.info("Initializing Runic Overlord systems...")

        register(plugin, SandBreakListener())
        register(plugin, VoucherActionListener)
        register(plugin, RuneApplyListener())
        register(plugin, RuneEffectListener())
        register(plugin, RuneRemoverGuiListener)
        register(plugin, RuneProtectionListener())
        register(plugin, RuneLootInjector)
        register(plugin, BlinkstepListener())
        register(plugin, BarrierWand())
        register(plugin, RtpListener())
        register(plugin, IdentifierUseListener())
        register(plugin, CustomFishingListener<Any>())
        register(plugin, CustomFishingGearListener())
        register(plugin, GearProtectionListener)
        register(plugin, CustomGearListener)
        register(plugin, QualityRerollVoucherListener())
        register(plugin, VoucherUseListener())


        register(plugin, EnchantUpgradeVoucherListener())
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
