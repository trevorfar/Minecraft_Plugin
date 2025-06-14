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

    /* ───────────────────────── infrastructure ──────────────────────── */
    private val plugin = Bukkit.getPluginManager().getPlugin("RunicOverlord")!!
    private val bars   = mutableMapOf<Player, BossBar>()

    /* =================================================================== */
    /*                       PUBLIC API – CALLERS                          */
    /* =================================================================== */

    /**
     * Called from the *new* `Probability` pipeline.
     *
     * The `roll` already contains a target rarity + tier, so we filter
     * the master fish list to that bucket **and** to player‐level / biome
     * constraints.  If nothing matches we simply return `null`.
     */
    fun giveFish(
        p     : Player,
        biome : Biome,
        roll  : FishRewardSpec
    ): ItemStack? {

        /* —— find a fish that matches rarity + tier bucket —— */
        val lvl = FishingLevel.FishingLeveling.level(p)
        val spec = FishingConfig.fish.values
            .filter { it.rarity == roll.rarity && it.tier == roll.tier }
            .filter { it.minLevel <= lvl && (it.biomes.isEmpty() || biome in it.biomes) }
            .randomOrNull() ?: return null            // dead-bucket, nothing rolled

        /* —— map roll.rarity → TierSpec (“common”, “rare”, …) —— */
        val tier = FishingConfig.tiers[roll.rarity.name.lowercase()]
            ?: FishingConfig.randomTier()             // fallback so it never crashes

        return createFishStack(p, spec, tier)
    }

    /**
     * Legacy overload – still used by e.g. /instantCatch or tests.
     * Keeps prior behaviour (random tier within level gate).
     */
    fun giveFish(p: Player, biome: Biome): ItemStack? {
        val spec = FishingConfig.randomFishForPlayer(p, biome) ?: return null
        val lvl  = FishingLevel.FishingLeveling.level(p)

        var tier: TierSpec
        do { tier = FishingConfig.randomTier() } while (!tierAllowed(lvl, tier.id))

        return createFishStack(p, spec, tier)
    }

    /* Quick helper for Willow’s Rod */
    fun instantCatch(p: Player, biome: Biome) {
        val caught = giveFish(p, biome) ?: return
        p.sendMessage("§aWillow’s Rod reels in a fish instantly!")
        p.playSound(p.location, Sound.ENTITY_ITEM_PICKUP, 0.7f, 1.4f)
    }

    /* =================================================================== */
    /*                   INTERNAL BUILD-STACK HELPERS                      */
    /* =================================================================== */

    private fun createFishStack(p: Player, spec: FishSpec, tier: TierSpec): ItemStack {
        val weight = spec.randomWeight(tier.minWeightPercent)

        val sellValue =
            weight.pow(PriceModel.WEIGHT_EXP) *
                    spec.baseValue *
                    PriceModel.FACTOR *
                    tier.multiplier
        val weightStr  = String.format("%.2f", weight)

        val item = ItemStack(spec.material).apply {
            itemMeta = itemMeta!!.apply {
                setDisplayName("${tier.colour}${tier.pretty} ${spec.displayName} §7($weightStr kg)")
                lore = listOf(
                    "§8Tier Mult: ×${tier.multiplier}",
                    "§8Sell: §e$${"%,2f".format(sellValue)}"
                )
                persistentDataContainer.apply {
                    set(FishingKeys.FISH_ID,     PersistentDataType.STRING,  spec.id)
                    set(FishingKeys.FISH_WEIGHT, PersistentDataType.DOUBLE,  weight)
                    set(FishingKeys.FISH_TIER,   PersistentDataType.STRING,  tier.id)
                }
            }
        }

        give(p, item)
        awardXp(p, weight, tier)
        maybeBroadcast(p, spec, tier, weightStr)
        return item
    }

    /* XP & broadcast copied unmodified from original implementation */
    private fun awardXp(p: Player, weight: Double, tier: TierSpec) {
        val xpFactor = when (tier.id.lowercase()) {
            "common"   -> 1.0
            "uncommon" -> 2.5
            "rare"     -> 5.5
            "epic"     -> 15.0
            "mythic"   -> 25.0
            else       -> 1.0
        }
        val rawXp = weight * 10 * xpFactor * 2.0

        if (FishingLevel.FishingLeveling.addXp(p, rawXp)) {
            p.sendMessage("§bFishing Level Up! You are now §e${FishingLevel.FishingLeveling.level(p)}§b.")
            p.playSound(p.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f)
        }
        showProgressBar(p)
    }

    private fun maybeBroadcast(p: Player, spec: FishSpec, tier: TierSpec, weightStr: String) {
        if (!tier.broadcast) return
        Bukkit.broadcast(
            Component.text("${p.name} reeled in a ${tier.colour}${tier.pretty} ${spec.displayName} §7($weightStr kg)!"),
            ""
        )
    }

    /* ──────────── UI helper ──────────── */
    fun showProgressBar(p: Player) {
        val (xp, need) = FishingLevel.FishingLeveling.progress(p)
        val frac       = (xp / need).coerceIn(0.0, 1.0)

        val bar = bars.computeIfAbsent(p) {
            Bukkit.createBossBar("§bFishing XP", BarColor.BLUE, BarStyle.SEGMENTED_20)
                .apply { addPlayer(p) }
        }
        bar.progress = frac
        bar.setTitle("§bFishing Lv ${FishingLevel.FishingLeveling.level(p)} §7– ${"%.0f".format(frac * 100)}%")

        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            bar.removeAll(); bars -= p
        }, 20 * 6)
    }

    /* ─────────────── helper: tier gate for Epic/Mythic ─────────── */
    fun tierAllowed(level: Int, id: String) = when (id.lowercase()) {
        "epic"   -> level >= RewardCategory.APPRENTICE.min   // ≥ 10
        "mythic" -> level >= RewardCategory.VETERAN.min      // ≥ 25
        else     -> true
    }

    /* ─────────────── convenience give helpers ─────────── */
    fun give(p: Player, item: ItemStack) {
        val leftovers = p.inventory.addItem(item)
        if (leftovers.isEmpty()) {
            p.playSound(p.location, Sound.ENTITY_ITEM_PICKUP, 0.6f, 1.25f)
        } else {
            leftovers.values.forEach { drop -> p.world.dropItemNaturally(p.location, drop) }
        }
    }

    fun giveCustomItem(p: Player, spec: ItemRewardSpec): ItemStack? {
        val amount = Random.nextInt(spec.min, spec.max + 1)
        val item = ItemStack(spec.material, amount).apply {
            spec.name?.let { display ->
                itemMeta = itemMeta!!.apply { setDisplayName(display) }
            }
        }
        give(p, item)
        return item
    }
}
