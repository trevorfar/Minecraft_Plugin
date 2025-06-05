package com.trevorfarias.runic_overlord.util

import net.milkbowl.vault.economy.Economy
import org.bukkit.OfflinePlayer
import org.bukkit.plugin.RegisteredServiceProvider
import org.bukkit.plugin.java.JavaPlugin

/**
 * Thin convenience wrapper around Vault's Economy service.
 * Call [setup] once during plugin enable; thereafter use [deposit] or [withdraw].
 */
object VaultUtil {
    private var econ: Economy? = null

    /** Returns true if an Economy provider was successfully grabbed. */
    fun setup(plugin: JavaPlugin): Boolean {
        val rsp: RegisteredServiceProvider<Economy> =
            plugin.server.servicesManager.getRegistration(Economy::class.java) ?: return false
        econ = rsp.provider
        return econ != null
    }

    fun deposit(player: OfflinePlayer, amount: Double) {
        econ?.depositPlayer(player, amount)
    }

    fun withdraw(player: OfflinePlayer, amount: Double) {
        econ?.withdrawPlayer(player, amount)
    }

    fun format(amount: Double): String =
        econ?.format(amount) ?: String.format("$%,.2f", amount)

    val isEnabled: Boolean
        get() = econ != null
}
