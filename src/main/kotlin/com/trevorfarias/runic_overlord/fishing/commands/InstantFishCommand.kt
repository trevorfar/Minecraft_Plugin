/* com/trevorfarias/runic_overlord/fishing/commands/InstantFishCommand.kt */
package com.trevorfarias.runic_overlord.fishing.commands

import com.trevorfarias.runic_overlord.fishing.*
import com.trevorfarias.runic_overlord.fishing.FishingRewards.give
import com.trevorfarias.runic_overlord.fishing.FishingRewards.giveCustomItem
import com.trevorfarias.runic_overlord.gear.GearFactory
import com.trevorfarias.runic_overlord.runes.RuneFactory
import com.trevorfarias.runic_overlord.voucher.VoucherFactory
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import kotlin.math.pow
import java.util.concurrent.ThreadLocalRandom

class InstantFishCommand : CommandExecutor {

    override fun onCommand(
        sender: CommandSender,
        cmd: Command,
        label: String,
        args: Array<out String>
    ): Boolean {

        val p = sender as? Player ?: return true
        val biome = p.location.block.biome

        // Use new global probability roll instead of per-table
        val reward = Probability(FishingConfig.allRewardSpecs()).roll()

        fun announce(msg: String) =
            Bukkit.broadcast(msg, "runicoverlord.seeBroadcast")

        Bukkit.getLogger().info("REWARD:  $reward")

        when (reward) {
            is FishRewardSpec -> {
                val caught = FishingRewards.giveFish(p, biome)
                if (caught != null && reward.broadcast)
                    announce("${p.displayName} §7caught ${caught.itemMeta.displayName}§7!")
            }

            is GearRewardSpec -> {
                val item = if (reward.unidentified)
                    GearFactory.createUnidentified(reward.gearId)
                else
                    GearFactory.create(reward.gearId)
                item?.let {
                    give(p, it)
                    if (reward.broadcast)
                        announce("${p.displayName} §dfound ${it.itemMeta.displayName}§d!")
                }
            }

            is RuneRewardSpec -> RuneFactory.createRune(reward.runeId)?.let { item ->
                give(p, item)
                if (reward.broadcast)
                    announce("${p.displayName} §areeled in the rune §l${item.itemMeta.displayName}§r§a!")
            }

            is VoucherRewardSpec -> VoucherFactory.createVoucher(reward.voucherId)?.let { item ->
                give(p, item)
                if (reward.broadcast)
                    announce("${p.displayName} §bfound a voucher: §l${item.itemMeta.displayName}§r§b!")
            }

            is ItemRewardSpec -> {
                val item = giveCustomItem(p, reward) ?: return false
                if (reward.broadcast)
                    announce("${p.displayName} §dhooked ${item.itemMeta.displayName}§d!")
            }

            null -> p.sendMessage("§7No reward matched your roll.")
        }
    return true
    }
}
