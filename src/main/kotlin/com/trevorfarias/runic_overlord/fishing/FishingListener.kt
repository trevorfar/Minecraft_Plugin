package com.trevorfarias.runic_overlord.fishing

import com.trevorfarias.runic_overlord.runes.RuneFactory
import com.trevorfarias.runic_overlord.voucher.VoucherFactory
import com.trevorfarias.runic_overlord.fishing.model.*
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import roll
import kotlin.random.Random

class CustomFishingListener<Biome : Any> : Listener {

    @EventHandler
    fun onCatch(event: PlayerFishEvent) {
        if (event.state != PlayerFishEvent.State.CAUGHT_FISH) return
        val player = event.player
        val biome  = player.location.block.biome

        val table = FishingConfig.tableForBiome(biome)

        repeat(table.rolls) {
            when (val reward = table.roll()) {
                is FishRewardSpec   -> giveFish(player, biome)
                is RuneRewardSpec   -> RuneFactory.createRune(reward.runeId)?.let { give(player, it) }
                is VoucherRewardSpec-> VoucherFactory.createVoucher(reward.voucherId)?.let { give(player, it) }
                is ItemRewardSpec   -> giveCustomItem(player, reward)
            }
        }

        // suppress vanilla fish drop
        (event.caught as? Item)?.remove()
    }

    /* ---------- fish creation ---------- */

    private fun giveFish(p: Player, biome: org.bukkit.block.Biome) {
        val spec   = FishingConfig.randomFishForBiome(biome) ?: return
        val tier   = FishingConfig.randomTier()
        val weight = spec.randomWeight(tier.minWeightPercent)

        val sellValue = weight * spec.baseValue * tier.multiplier

        val item = ItemStack(spec.material).apply {
            itemMeta = itemMeta!!.apply {
                setDisplayName("${tier.colour}${tier.pretty} ${spec.displayName} §7(${weight} kg)")
                lore = listOf(
                    "§8Tier Mult: ×${tier.multiplier}",
                    "§8Sell: ${
                        String.format("§e$%,.2f", sellValue)
                    }"
                )
                persistentDataContainer.apply {
                    set(FishingKeys.FISH_ID,     PersistentDataType.STRING,  spec.id)
                    set(FishingKeys.FISH_WEIGHT, PersistentDataType.DOUBLE,  weight)
                    set(FishingKeys.FISH_TIER,   PersistentDataType.STRING,  tier.id)
                }
            }
        }

        give(p, item)           // ‘give’ now adds to inventory first
        rewardXp(p, weight)

        // global broadcast for Mythical (or any tier where broadcast=true)
        if (tier.broadcast) {
            Bukkit.broadcast(
                Component.text("${p.name} reeled in a ${tier.colour}${tier.pretty} ${spec.displayName} §7(${weight} kg)!"),
                ""
            )
        }
    }


    /* ---------- NEW: generic item reward ---------- */

    private fun giveCustomItem(p: Player, spec: ItemRewardSpec) {
        if (Random.nextDouble() > spec.chance) return  // shouldn’t happen, but double-secure
        val amount   = Random.nextInt(spec.min, spec.max + 1)
        val item     = ItemStack(spec.material, amount)
        spec.name?.let { display ->
            item.itemMeta = item.itemMeta!!.apply { setDisplayName(display) }
        }
        give(p, item)
    }


    private fun give(p: Player, item: ItemStack) {
        val leftovers = p.inventory.addItem(item)      // returns map if no space
        if (leftovers.isEmpty()) {
            p.updateInventory()
            p.playSound(p.location, Sound.ENTITY_ITEM_PICKUP, 0.6f, 1.25f)
        } else {
            leftovers.values.forEach { drop ->
                p.world.dropItemNaturally(p.location, drop)
            }
        }
    }

    /** Placeholder XP logic – swap for your own levelling system later. */
    private fun rewardXp(p: Player, weight: Double) {
        p.giveExp((weight * 2).toInt())   // 2 XP per kg, tweak as desired
    }
}
