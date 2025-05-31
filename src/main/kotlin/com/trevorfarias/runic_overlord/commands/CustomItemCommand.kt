package com.trevorfarias.runic_overlord.commands

import com.trevorfarias.runic_overlord.runes.RuneFactory
import com.trevorfarias.runic_overlord.voucher.VoucherFactory
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CustomItemCommand : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("Only players can use this command.")
            return true
        }

        if (!sender.hasPermission("runicoverlord.admin")) {
            sender.sendMessage("§cYou do not have permission to use this command.")
            return true
        }

        if (args.size < 2) {
            sender.sendMessage("§cUsage: /givecustom <voucher|rune> <id>")
            return true
        }

        val type = args[0].lowercase()
        val id = args[1].lowercase()

        val item = when (type) {
            "voucher" -> VoucherFactory.createVoucher(id)
            "rune" -> RuneFactory.createRune(id)
            else -> {
                sender.sendMessage("§cUnknown item type: $type")
                return true
            }
        }

        if (item == null) {
            sender.sendMessage("§cUnknown ${type} ID: $id")
            return true
        }

        sender.inventory.addItem(item)
        sender.sendMessage("§aYou have been given a ${item.itemMeta?.displayName}")
        return true
    }
}
