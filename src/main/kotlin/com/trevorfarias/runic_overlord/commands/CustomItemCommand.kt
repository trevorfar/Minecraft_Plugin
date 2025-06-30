
package com.trevorfarias.runic_overlord.commands

import com.trevorfarias.runic_overlord.gear.GearFactory
import com.trevorfarias.runic_overlord.identifier.IdentifierFactory
import com.trevorfarias.runic_overlord.listeners.BarrierWand
import com.trevorfarias.runic_overlord.runes.RuneFactory
import com.trevorfarias.runic_overlord.voucher.VoucherFactory
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * /givecustom <voucher|rune|gear|identifier|wand> <id?> [unidentified]
 */
class CustomItemCommand : TabExecutor {

    /* ------------------------------------------------------------------ */
    /*  onCommand – actually gives the item                               */
    /* ------------------------------------------------------------------ */
    override fun onCommand(
        sender : CommandSender,
        cmd    : Command,
        label  : String,
        args   : Array<out String>
    ): Boolean {

        /* must be run by a player */
        val player = sender as? Player
        if (player == null) {
            sender.sendMessage("§cOnly players may use this command.")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("§cUsage: /$label <voucher|rune|gear> <id> [unidentified]")
            return true
        }

        val type = args[0].lowercase()
        val id   = args.getOrNull(1)?.lowercase() ?: run {
            sender.sendMessage("§cYou must specify an id.")
            return true
        }

        val item: ItemStack? = when (type) {

            "voucher"    -> id.let { VoucherFactory.createVoucher(it) }

            "rune"       -> id.let { RuneFactory.createRune(it) }

            "gear"    -> {
                val unidentified = args.getOrNull(2)?.equals("unidentified", true) == true
                if (unidentified) GearFactory.createUnidentified(id)
                else               GearFactory.create(id)
            }

            "identifier" -> IdentifierFactory.create()
            "wand"       -> BarrierWand.createWand()


            else -> {
                sender.sendMessage("§cUnknown type \"$type\".")
                null
            }
        }

        if (item == null) {
            sender.sendMessage("§cNo such $type id \"$id\".")
            return true
        }

        player.inventory.addItem(item)
        sender.sendMessage("§aGave you 1× §f${item.itemMeta?.displayName ?: id}§a.")
        return true
    }

    /* ------------------------------------------------------------------ */
    /*  onTabComplete – live suggestions without hard-coding ids          */
    /* ------------------------------------------------------------------ */
    override fun onTabComplete(
        sender : CommandSender,
        cmd    : Command,
        alias  : String,
        args   : Array<out String>
    ): MutableList<String>? {

        /* helper – filter + sort */
        fun Iterable<String>.match(partial: String) =
            filter { it.startsWith(partial, ignoreCase = true) }.sorted()

        // /givecustom <type>
        if (args.size == 1) {
            return listOf("voucher", "rune", "gear", "identifier", "wand")
                .match(args[0])
                .toMutableList()
        }

        // /givecustom <type> <id>
        if (args.size == 2) {
            return when (args[0].lowercase()) {
                "voucher" -> VoucherFactory.allIds().match(args[1]).toMutableList()
                "rune"    -> RuneFactory.allIds().match(args[1]).toMutableList()
                "gear"    -> GearFactory.allIds().match(args[1]).toMutableList()
                else      -> mutableListOf()
            }
        }

        // /givecustom gear <id> <unidentified?>
        if (args.size == 3 && args[0].equals("gear", true)) {
            return listOf("unidentified").match(args[2]).toMutableList()
        }

        return mutableListOf()
    }
}
