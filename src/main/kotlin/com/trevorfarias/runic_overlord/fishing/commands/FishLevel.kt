package com.trevorfarias.runic_overlord.fishing.commands

import com.trevorfarias.runic_overlord.fishing.FishingLevel
import com.trevorfarias.runic_overlord.fishing.RewardCategory
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class FishLevel : CommandExecutor {
    override fun onCommand(sender: CommandSender, cmd: Command, lbl: String, args: Array<out String>): Boolean {
        if (sender !is Player) { sender.sendMessage("Players only."); return true }

        val (xp, need) = FishingLevel.FishingLeveling.progress(sender)
        val level = FishingLevel.FishingLeveling.level(sender)
        val tier  = RewardCategory.forLevel(level)
        val prettyTier = tier.name.lowercase().replaceFirstChar { it.titlecase() }
        sender.sendMessage("§bFishing Level: §e$level §l§9$prettyTier")
        sender.sendMessage("§7Progress: §a${"%.1f".format(xp)}/${"%.1f".format(need)} XP")
        return true
    }
}
