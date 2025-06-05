package com.trevorfarias.runic_overlord.fishing.commands

import com.trevorfarias.runic_overlord.fishing.FishingConfig
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class FishingAdminCommand : CommandExecutor {

    override fun onCommand(
        sender: CommandSender,
        cmd: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        // Permission is already checked by plugin.yml, but double-gate for safety
        Bukkit.getLogger().info("[DEBUG] FishingAdminCommand executed by ${sender.name}")

        if (sender !is Player || !sender.hasPermission("runicoverlord.admin")) {
            sender.sendMessage("§cYou don’t have permission.")
            return true
        }

        if (args.size == 1 && args[0].equals("reload", ignoreCase = true)) {
            FishingConfig.reload()
            sender.sendMessage("§a[RunicOverlord] Fishing config reloaded.")
            return true
        }

        sender.sendMessage("§eUsage: /rofishing reload")
        return true
    }
}
