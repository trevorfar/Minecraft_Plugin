package com.trevorfarias.runic_overlord

import com.trevorfarias.runic_overlord.fishing.CustomFishingListener
import com.trevorfarias.runic_overlord.fishing.FishingConfig
import com.trevorfarias.runic_overlord.fishing.FishingLevel
import com.trevorfarias.runic_overlord.fishing.commands.FishLevel
import com.trevorfarias.runic_overlord.fishing.commands.FishingAdminCommand
import com.trevorfarias.runic_overlord.fishing.commands.SellFishCommand
import com.trevorfarias.runic_overlord.fishing.commands.SetLevel
import com.trevorfarias.runic_overlord.gear.GearFactory
import com.trevorfarias.runic_overlord.util.VaultUtil
import org.bukkit.plugin.java.JavaPlugin

class RunicOverlord : JavaPlugin() {

    override fun onEnable() {
        instance = this
        logger.info("Runic Overlord is up and ready!")
        PluginManager.initialize(this)
        FishingConfig.load(this)
        getCommand("sellfish")?.setExecutor(SellFishCommand())
        getCommand("rofishing")?.setExecutor(FishingAdminCommand())
            ?: logger.severe("COMMAND rofishing not found in plugin.yml")
        getCommand("fishlevel")?.setExecutor(FishLevel())
        getCommand("rofsetlevel")?.setExecutor(SetLevel())
        GearFactory.initDefaults()
        FishingLevel.FishingDataKeys.init(this)
        if (!VaultUtil.setup(this)) {
            logger.warning("No Vault economy provider found – /sellfish will just drop XP.")
        }    }

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
