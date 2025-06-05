package com.trevorfarias.runic_overlord.fishing.commands

import com.trevorfarias.runic_overlord.fishing.FishingConfig
import com.trevorfarias.runic_overlord.fishing.FishingKeys
import com.trevorfarias.runic_overlord.util.VaultUtil
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType

class SellFishCommand : CommandExecutor {

    override fun onCommand(
        sender: CommandSender,
        cmd: Command,
        label: String,
        args: Array<out String>
    ): Boolean {

        val p = sender as? Player ?: return true
        val inv = p.inventory

        var total = 0.0

        for (slot in 0 until inv.size) {
            val item = inv.getItem(slot) ?: continue
            val meta = item.itemMeta ?: continue

            /*────────── read PDC tags ──────────*/
            val id       = meta.persistentDataContainer.get(FishingKeys.FISH_ID,     PersistentDataType.STRING) ?: continue
            val weight   = meta.persistentDataContainer.get(FishingKeys.FISH_WEIGHT, PersistentDataType.DOUBLE) ?: continue
            val tierId   = meta.persistentDataContainer.get(FishingKeys.FISH_TIER,   PersistentDataType.STRING) ?: "common"

            val spec = FishingConfig.fish[id]            ?: continue
            val tier = FishingConfig.tiers[tierId]       ?: FishingConfig.tiers["common"]!!

            /*────────── compute price ───────────*/
            val pricePerFish = weight * spec.baseValue * tier.multiplier
            total += pricePerFish * item.amount

            inv.setItem(slot, null) // clear the slot
        }

        if (total <= 0.0) {
            p.sendMessage("§eYou have no fish to sell!")
            return true
        }

        if (VaultUtil.isEnabled) {
            VaultUtil.deposit(p, total)
            p.sendMessage("§aSold catch for §e${VaultUtil.format(total)}")
        } else {
            val xp = (total * 0.5).toInt()
            p.giveExp(xp)
            p.sendMessage("§aNo economy plugin found; you gained §e$xp XP §ainstead.")
        }
        return true
    }
}
