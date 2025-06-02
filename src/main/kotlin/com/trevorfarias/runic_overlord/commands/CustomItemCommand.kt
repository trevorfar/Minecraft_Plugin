package com.trevorfarias.runic_overlord.commands

import com.trevorfarias.runic_overlord.gear.GearFactory          // ← NEW
import com.trevorfarias.runic_overlord.identifier.IdentifierFactory // ← NEW
import com.trevorfarias.runic_overlord.listeners.BarrierWand
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
        if (args.isEmpty()) {
            sender.sendMessage("§cUsage: /givecustom <voucher|rune|wand|gear|identifier> <id?>")
            return true
        }

        val type = args[0].lowercase()
        val id   = args.getOrNull(1)?.lowercase()

        val item = when (type) {
            // ───────── existing cases ─────────
            "voucher"    -> id?.let { VoucherFactory.createVoucher(it) }
            "rune"       -> id?.let { RuneFactory.createRune(it) }
            "wand"       -> BarrierWand.createWand()

            // ────────── NEW: gear ────────────
            "gear" -> {
                if (id == null) {
                    sender.sendMessage("§cUsage: /givecustom gear <id> [unidentified]")
                    return true
                }

                //  /givecustom gear silver_sword unidentified
                if (args.getOrNull(2)?.equals("unidentified", true) == true)
                    GearFactory.createUnidentified(id)
                else
                    GearFactory.create(id)          // ← rolls random quality now
            }

            // ──────── NEW: identifier ────────
            "identifier" -> IdentifierFactory.create()

            else -> {
                sender.sendMessage("§cUnknown item type: $type")
                return true
            }
        }

        if (item == null) {
            sender.sendMessage("§cUnknown $type id: $id")
            return true
        }

        sender.inventory.addItem(item)
        sender.sendMessage("§aYou received: ${item.itemMeta?.displayName}")
        return true
    }
}
