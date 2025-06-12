package com.trevorfarias.runic_overlord.gear

import com.trevorfarias.runic_overlord.gear.GearFactory.reloadFromYML
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin

class GearReloadCommand(private val plugin: JavaPlugin) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isNotEmpty() && args[0].equals("reload", ignoreCase = true)) {
            GearFactory.reloadFromYML(plugin)
            sender.sendMessage("§aGear reloaded from gear.yml!")
            return true
        }
        sender.sendMessage("§cUsage: /rofgear reload")
        return true
    }
}
