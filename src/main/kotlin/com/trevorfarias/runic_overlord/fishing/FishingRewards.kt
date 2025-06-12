package com.trevorfarias.runic_overlord.fishing

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.block.Biome
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import kotlin.math.pow
import kotlin.random.Random

object FishingRewards {
    private val plugin = Bukkit.getPluginManager().getPlugin("RunicOverlord")!!
    private val bars   = mutableMapOf<Player, BossBar>()

    fun showProgressBar(p: Player) {
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

    fun giveFish(p: Player, biome: Biome): ItemStack? {
        val spec = FishingConfig.randomFishForPlayer(p, biome) ?: return null
        val lvl  = FishingLevel.FishingLeveling.level(p)

        var tier: TierSpec
        do { tier = FishingConfig.randomTier() } while (!tierAllowed(lvl, tier.id))

        val weight = spec.randomWeight(tier.minWeightPercent)

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
            "mythic" -> 25.0
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

    fun instantCatch(p: Player, biome: Biome) {
        val caught = giveFish(p, biome) ?: return
        p.sendMessage("§aWillow’s Rod reels in a fish instantly!")
        p.playSound(p.location, Sound.ENTITY_ITEM_PICKUP, 0.7f, 1.4f)
    }

    /* ─────────────── helper: tier gate for Epic/Mythical ─────────── */
     fun tierAllowed(level: Int, id: String) = when (id.lowercase()) {
        "epic"     -> level >= RewardCategory.APPRENTICE.min   // ≥ 10
        "mythic" -> level >= RewardCategory.VETERAN.min      // ≥ 25
        else       -> true
    }

     fun give(p: Player, item: ItemStack) {
        val leftovers = p.inventory.addItem(item)
        if (leftovers.isEmpty()) {
            p.playSound(p.location, Sound.ENTITY_ITEM_PICKUP, 0.6f, 1.25f)
        } else {
            leftovers.values.forEach { drop -> p.world.dropItemNaturally(p.location, drop) }
        }
    }
    fun giveCustomItem(p: Player, spec: ItemRewardSpec): ItemStack? {
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


}