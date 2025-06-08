package com.trevorfarias.runic_overlord.fishing

import com.trevorfarias.runic_overlord.fishing.model.*
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
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import roll
import kotlin.math.pow
import kotlin.random.Random

class CustomFishingListener<T> : Listener {

    /* ─────────────────────────── globals ─────────────────────────── */
    private val plugin = Bukkit.getPluginManager().getPlugin("RunicOverlord")!!
    private val bars   = mutableMapOf<Player, BossBar>()

    /* ───────────────────── boss-bar helper ───────────────────────── */
    private fun showProgressBar(p: Player) {
        val (xp, need) = FishingLevel.FishingLeveling.progress(p)
        val frac       = (xp / need).coerceIn(0.0, 1.0)

        val bar = bars.computeIfAbsent(p) {
            Bukkit.createBossBar("§bFishing XP", BarColor.BLUE, BarStyle.SEGMENTED_20)
                .apply { addPlayer(p) }
        }
        bar.progress = frac
        bar.setTitle(
            "§bFishing Lv ${FishingLevel.FishingLeveling.level(p)} §7– ${String.format("%.0f", frac * 100)}%"
        )

        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            bar.removeAll(); bars -= p
        }, 20 * 6)
    }

    /* ─────────────── helper: tier gate for Epic/Mythical ─────────── */
    private fun tierAllowed(level: Int, id: String) = when (id.lowercase()) {
        "epic"     -> level >= RewardCategory.APPRENTICE.min   // ≥ 10
        "mythical" -> level >= RewardCategory.VETERAN.min      // ≥ 25
        else       -> true
    }

    /* ─────────────────────── main catch hook ─────────────────────── */
    @EventHandler
    fun onCatch(event: PlayerFishEvent) {
        if (event.state != PlayerFishEvent.State.CAUGHT_FISH) return
        val player = event.player
        val biome  = player.location.block.biome
        val table  = FishingConfig.tableForBiome(biome)

        repeat(table.rolls) {

            val reward = FishingConfig.randomRewardFor(player, table)

            fun announce(msg: String) =
                Bukkit.broadcast(msg, "runicoverlord.seeBroadcast") // null => everyone

            when (reward) {

                /* ────────── FISH ────────── */
                is FishRewardSpec -> {
                    val caught = giveFish(player, biome)               // ItemStack returned
                    if (reward.broadcast) {
                        if (caught != null) {
                            announce("${player.displayName} §7caught ${caught.itemMeta.displayName}§7!")
                        }
                    }
                }

                /* ─────────── RUNE ────────── */
                is RuneRewardSpec -> RuneFactory.createRune(reward.runeId)?.let { item ->
                    give(player, item)
                    if (reward.broadcast) {
                        announce("${player.displayName} §areeled in the rune §l${item.itemMeta.displayName}§r§a!")
                    }
                }

                /* ───────── VOUCHER ───────── */
                is VoucherRewardSpec -> VoucherFactory.createVoucher(reward.voucherId)?.let { item ->
                    give(player, item)
                    if (reward.broadcast) {
                        announce("${player.displayName} §bfound a voucher: §l${item.itemMeta.displayName}§r§b!")
                    }
                }

                /* ─────── CUSTOM ITEM ─────── */
                is ItemRewardSpec -> {
                    val item = giveCustomItem(player, reward) ?: return@repeat
                    if (reward.broadcast) {
                        announce("${player.displayName} §dhooked ${item.itemMeta.displayName}§d!")
                    }
                }
            }
        }

        (event.caught as? Item)?.remove() // suppress vanilla item drop
    }

    /* ─────────────────────── fish creation ───────────────────────── */
    private fun giveFish(p: Player, biome: Biome): ItemStack? {
        val spec = FishingConfig.randomFishForPlayer(p, biome) ?: return null
        val lvl  = FishingLevel.FishingLeveling.level(p)

        var tier: TierSpec
        do { tier = FishingConfig.randomTier() } while (!tierAllowed(lvl, tier.id))

        val weight     = spec.randomWeight(tier.minWeightPercent)

        val sellValue =
            weight.pow(PriceModel.WEIGHT_EXP) *
                    spec.baseValue *
                    PriceModel.FACTOR *
                    tier.multiplier
        val weightStr  = String.format("%.2f", weight)


        val item = ItemStack(spec.material).apply {
            itemMeta = itemMeta!!.apply {
                setDisplayName("${tier.colour}${tier.pretty} ${spec.displayName} §7(${weightStr} kg)")
                lore = listOf(
                    "§8Tier Mult: ×${tier.multiplier}",
                    "§8Sell: §e$${"%,.2f".format(sellValue)}"
                )
                persistentDataContainer.apply {
                    set(FishingKeys.FISH_ID,     PersistentDataType.STRING,  spec.id)
                    set(FishingKeys.FISH_WEIGHT, PersistentDataType.DOUBLE,  weight)
                    set(FishingKeys.FISH_TIER,   PersistentDataType.STRING,  tier.id)
                }
            }
        }

        give(p, item)

        /* XP gain */
        val xpFactor = when (tier.id.lowercase()) {
            "common"   -> 1.0
            "uncommon" -> 2.5
            "rare"     -> 5.5
            "epic"     -> 15.0
            "mythical" -> 25.0
            else       -> 1.0
        }
        val rawXp = weight * 10 * xpFactor * 2.0

        if (FishingLevel.FishingLeveling.addXp(p, rawXp)) {
            p.sendMessage("§bFishing Level Up! You are now §e${FishingLevel.FishingLeveling.level(p)}§b.")
            p.playSound(p.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f)
        }
        showProgressBar(p)

        /* Tier broadcast handled here so no duplication */
        if (tier.broadcast) {
            Bukkit.broadcast(
                Component.text("${p.name} reeled in a ${tier.colour}${tier.pretty} ${spec.displayName} §7(${weightStr} kg)!"),
                ""
            )
        }
        return item
    }

    /* ─────────────── custom item helper ─────────────── */
    private fun giveCustomItem(p: Player, spec: ItemRewardSpec): ItemStack? {
        if (Random.nextDouble() > spec.chance) return null
        val amount = Random.nextInt(spec.min, spec.max + 1)
        val item   = ItemStack(spec.material, amount).apply {
            spec.name?.let { display ->
                itemMeta = itemMeta!!.apply { setDisplayName(display) }
            }
        }
        give(p, item)
        return item
    }

    /* ─────────────── inventory helper ─────────────── */
    private fun give(p: Player, item: ItemStack) {
        val leftovers = p.inventory.addItem(item)
        if (leftovers.isEmpty()) {
            p.playSound(p.location, Sound.ENTITY_ITEM_PICKUP, 0.6f, 1.25f)
        } else {
            leftovers.values.forEach { drop -> p.world.dropItemNaturally(p.location, drop) }
        }
    }
}
