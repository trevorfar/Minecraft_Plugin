package com.trevorfarias.runic_overlord.fishing

import com.trevorfarias.runic_overlord.fishing.FishingRewards.give
import com.trevorfarias.runic_overlord.fishing.FishingRewards.giveCustomItem
import com.trevorfarias.runic_overlord.fishing.model.*
import com.trevorfarias.runic_overlord.gear.GearFactory
import com.trevorfarias.runic_overlord.runes.RuneFactory
import com.trevorfarias.runic_overlord.util.Constants
import com.trevorfarias.runic_overlord.voucher.VoucherFactory
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.block.Biome
import org.bukkit.entity.FishHook
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import roll
import java.util.*
import kotlin.math.pow
import kotlin.random.Random

class CustomFishingListener<T> : Listener {
    @EventHandler
    fun onCatch(event: PlayerFishEvent) {
        val p = event.player
        val biome = p.location.block.biome
        val mainId = GearFactory.getSpec(p.inventory.itemInMainHand)?.id

        /* ───────────── NORMAL CAUGHT_FISH flow ───────────── */
        if (event.state != PlayerFishEvent.State.CAUGHT_FISH) return

        val table = FishingConfig.tableForBiome(biome)

        repeat(table.rolls) {
            val reward = FishingConfig.randomRewardFor(p, table)

            /* Rod of Neptune – decide once per roll whether to duplicate */
            val duplicate = (mainId == "neptune_rod") && Math.random() < Constants.NEPTUNE_CHANCE

            if(duplicate) p.playSound(p.location, Sound.ENTITY_ITEM_PICKUP, 0.7f, 1.4f)


            fun announce(msg: String) =
                Bukkit.broadcast(msg, "runicoverlord.seeBroadcast") // null → everyone

            when (reward) {

                /* ────────── FISH ────────── */
                is FishRewardSpec -> {
                    val caught = FishingRewards.giveFish(p, biome)
                    if (duplicate && caught != null) give(p, caught.clone())
                    if (reward.broadcast && caught != null) {
                        announce("${p.displayName} §7caught ${caught.itemMeta.displayName}§7!")
                    }
                }

                /* ─────────── RUNE ─────────── */
                is RuneRewardSpec -> RuneFactory.createRune(reward.runeId)?.let { item ->
                    give(p, item)
                    if (duplicate) give(p, item.clone())
                    if (reward.broadcast) {
                        announce("${p.displayName} §areeled in the rune §l${item.itemMeta.displayName}§r§a!")
                    }
                }

                /* ────────── VOUCHER ───────── */
                is VoucherRewardSpec -> VoucherFactory.createVoucher(reward.voucherId)?.let { item ->
                    give(p, item)
                    if (duplicate) give(p, item.clone())
                    if (reward.broadcast) {
                        announce("${p.displayName} §bfound a voucher: §l${item.itemMeta.displayName}§r§b!")
                    }
                }

                /* ──────── CUSTOM ITEM ─────── */
                is ItemRewardSpec -> {
                    val item = giveCustomItem(p, reward) ?: return@repeat
                    if (duplicate) give(p, item.clone())
                    if (reward.broadcast) {
                        announce("${p.displayName} §dhooked ${item.itemMeta.displayName}§d!")
                    }
                }

                /* ─────────── GEAR ─────────── */
                is GearRewardSpec -> {
                    val item = if (reward.unidentified)
                        GearFactory.createUnidentified(reward.gearId)
                    else
                        GearFactory.create(reward.gearId)
                    item?.let {
                        give(p, it)
                        if (duplicate) give(p, it.clone())
                    }
                }
            }
        }

        (event.caught as? Item)?.remove()
    }

}
