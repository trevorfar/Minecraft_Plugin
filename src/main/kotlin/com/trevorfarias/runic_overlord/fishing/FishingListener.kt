package com.trevorfarias.runic_overlord.fishing

import com.trevorfarias.runic_overlord.fishing.FishingRewards.give
import com.trevorfarias.runic_overlord.fishing.FishingRewards.giveCustomItem
import com.trevorfarias.runic_overlord.gear.GearFactory
import com.trevorfarias.runic_overlord.runes.RuneFactory
import com.trevorfarias.runic_overlord.util.Constants
import com.trevorfarias.runic_overlord.voucher.VoucherFactory
import org.bukkit.Bukkit
import org.bukkit.entity.Item
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

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
        var dup: ItemStack ? = null

        when (reward) {

            is FishRewardSpec -> {
                val caught = FishingRewards.giveFish(p, biome, reward)   // <─ NEW overload
                dup = caught
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
                dup = item
                give(p, item)
                if (reward.broadcast)
                    announce("${p.displayName} §areeled in the rune §l${item.itemMeta.displayName}§r§a!")
            }

            is VoucherRewardSpec -> VoucherFactory.createVoucher(reward.voucherId)?.let { item ->
                dup = item
                give(p, item)
                if (reward.broadcast)
                    announce("${p.displayName} §bfound a voucher: §l${item.itemMeta.displayName}§r§b!")
            }

            is ItemRewardSpec -> {
                val item = giveCustomItem(p, reward) ?: return
                dup = item
                if (reward.broadcast)
                    announce("${p.displayName} §dhooked ${item.itemMeta.displayName}§d!")
            }

            null -> p.sendMessage("§7No reward matched your roll.")
        }

        run {
            val main     = p.inventory.itemInMainHand
            val mainSpec = GearFactory.getSpec(main)
            if (mainSpec?.id == "neptune_rod") {

                val quality = main.itemMeta?.persistentDataContainer
                    ?.get(Constants.GEAR_QUALITY_KEY, PersistentDataType.INTEGER) ?: 100
                val chance  = mainSpec.abilityBaseChance * (quality / 100.0)
                Bukkit.getLogger().info("CHANCE $chance")

                if (Math.random() < (chance / 100.0)) {
                    dup?.let { p.inventory.addItem(it.clone()) }      // give identical copy
                    p.sendMessage("§9[Neptune] The sea yields double!")
                }
            }
        }

        (event.caught as? Item)?.remove()
    }
}
