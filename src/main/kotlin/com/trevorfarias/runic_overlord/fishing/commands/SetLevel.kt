package com.trevorfarias.runic_overlord.fishing.commands

import com.trevorfarias.runic_overlord.fishing.FishingLevel
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType

/**
 * /rofsetlevel <player> <level>
 *
 * Requires permission runicoverlord.admin
 */
class SetLevel : CommandExecutor {

    override fun onCommand(
        sender: CommandSender,
        cmd: Command,
        label: String,
        args: Array<out String>
    ): Boolean {

        /* ── permission + arg checks ── */
        if (!sender.hasPermission("runicoverlord.admin")) {
            sender.sendMessage("§cYou don’t have permission.")
            return true
        }
        if (args.size != 2) {
            sender.sendMessage("§eUsage: /rofsetlevel <player> <level>")
            return true
        }

        val target = Bukkit.getPlayerExact(args[0])
        if (target == null) {
            sender.sendMessage("§cPlayer ${args[0]} not found (must be online).")
            return true
        }

        val newLevel = args[1].toIntOrNull()
        if (newLevel == null || newLevel < 1) {
            sender.sendMessage("§cLevel must be a positive number.")
            return true
        }

        /* ── write PDC ── */
        val pdc = target.persistentDataContainer
        pdc.set(
            FishingLevel.FishingDataKeys.LEVEL,
            PersistentDataType.INTEGER,
            newLevel
        )
        pdc.set(
            FishingLevel.FishingDataKeys.XP,
            PersistentDataType.DOUBLE,
            0.0
        )

        sender.sendMessage("§aSet ${target.name}’s fishing level to §e$newLevel§a.")
        if (sender != target)
            target.sendMessage("§bYour fishing level was set to §e$newLevel §bby an admin.")

        return true
    }
}
