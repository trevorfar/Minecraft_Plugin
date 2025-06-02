package com.trevorfarias.runic_overlord

import com.trevorfarias.runic_overlord.gear.GearFactory
import org.bukkit.plugin.java.JavaPlugin

class RunicOverlord : JavaPlugin() {

    override fun onEnable() {
        instance = this
        logger.info("Runic Overlord is up and ready!")
        PluginManager.initialize(this)
        GearFactory.initDefaults()
    }

    override fun onDisable() {
        PluginManager.shutdown(this)
        logger.info("Runic Overlord disabled.")
    }

    companion object {
        lateinit var instance: RunicOverlord
            private set

        const val VOUCHER_NAME = "§bDiamond Voucher"
    }
}
