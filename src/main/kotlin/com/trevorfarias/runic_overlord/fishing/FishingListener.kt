package com.trevorfarias.runic_overlord.fishing

import com.trevorfarias.runic_overlord.fishing.FishingRewards.give
import com.trevorfarias.runic_overlord.fishing.FishingRewards.giveCustomItem
import com.trevorfarias.runic_overlord.gear.GearFactory
import com.trevorfarias.runic_overlord.runes.RuneFactory
import com.trevorfarias.runic_overlord.voucher.VoucherFactory
import org.bukkit.Bukkit
import org.bukkit.entity.Item
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerFishEvent

class CustomFishingListener<T> : Listener {
    @EventHandler
    fun onCatch(event: PlayerFishEvent) {
        val p = event.player
        val biome = p.location.block.biome
        if (event.state != PlayerFishEvent.State.CAUGHT_FISH) return

        // Use new global probability roll instead of per-table
        val reward = Probability(FishingConfig.allRewardSpecs()).roll()

        fun announce(msg: String) =
            Bukkit.broadcast(msg, "runicoverlord.seeBroadcast")

        Bukkit.getLogger().info("REWARD:  $reward")

        when (reward) {
            is FishRewardSpec -> {
                val caught = FishingRewards.giveFish(p, biome, reward)   // <─ NEW overload
                if (caught != null && reward.broadcast) {
                    announce("${p.displayName} §7caught ${caught.itemMeta.displayName}§7!")
                }
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
                val item = giveCustomItem(p, reward) ?: return
                if (reward.broadcast)
                    announce("${p.displayName} §dhooked ${item.itemMeta.displayName}§d!")
            }

            null -> p.sendMessage("§7No reward matched your roll.")
        }

        (event.caught as? Item)?.remove()
    }
}
