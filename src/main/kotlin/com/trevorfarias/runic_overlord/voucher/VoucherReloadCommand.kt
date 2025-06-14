package com.trevorfarias.runic_overlord.voucher

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin

class VoucherReloadCommand(private val plugin: JavaPlugin) : CommandExecutor {

    override fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<out String>): Boolean {
        if (args.size == 1 && args[0].equals("reload", true)) {
            VoucherFactory.reload(plugin)
            sender.sendMessage("§aVouchers reloaded from vouchers.yml!")
            return true
        }
        sender.sendMessage("§cUsage: /rofvoucher reload")
        return true
    }
}
