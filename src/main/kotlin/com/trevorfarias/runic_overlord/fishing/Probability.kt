package com.trevorfarias.runic_overlord.fishing

import com.trevorfarias.runic_overlord.gear.Rarity
import com.trevorfarias.runic_overlord.gear.Tier
import org.bukkit.Bukkit


class Probability(val allRewards: List<RewardSpec>) { // Returns an Item, where item is either a GearRewardSpec, FishRewardSpec or VoucherRewardSpec

    enum class ParentCategory(val weight: Int) {
        FISH(90),
        ITEM(5),
        VOUCHER(5)

        //  FISH(90),
        //        ITEM(5),
        //        VOUCHER(5)
    }


    fun roll(): RewardSpec? {
        // ── 1. pick the *parent* category (Fish / Item / Voucher) ───────────
        val parent = pickWeightedRandom(ParentCategory.values().toList()) { it.weight } ?: return null
        Bukkit.getLogger().info("PARENT: $parent")

        val categoryFiltered = allRewards.filter {
            when (parent) {
                ParentCategory.FISH    -> it is FishRewardSpec
                ParentCategory.ITEM    -> it is GearRewardSpec
                ParentCategory.VOUCHER -> it is VoucherRewardSpec
            }
        }

        // ── 2. pick rarity + *starting* tier as usual ───────────────────────
        val rarity      = pickWeightedRandom(Rarity.values().toList()) { it.weight } ?: return null
        val startTier   = pickWeightedRandom(Tier.values().toList())   { it.weight } ?: return null
        val tiers       = Tier.values()                        // [I, II, III] (in declaration order)
        val startIdx    = tiers.indexOf(startTier)

        // ── 3. walk through the other tiers in a circular fashion ───────────
        for (offset in tiers.indices) {
            val tier = tiers[(startIdx + offset) % tiers.size]

            val candidates = categoryFiltered.filter {
                (it as? RewardTyped)?.let { r -> r.rarity == rarity && r.tier == tier } ?: false
            }

            if (candidates.isNotEmpty()) {
                Bukkit.getLogger().info("CANDIDATES @ $rarity / $tier = ${candidates.size}")
                return candidates.random()
            }
        }

        // ── 4. nothing found in this category → plain re-roll ───────────────
        return roll()        // let rollGuaranteed()’s maxAttempts prevent loops
    }


    // Takes a category with an associated rarity i.e., Fish: 0.9 item: 0.5 voucher 0.5 and selects a sub category
    fun <T> pickWeightedRandom(options: List<T>, weightSelector: (T) -> Int): T? {
        val totalWeight = options.sumOf(weightSelector)
        if (totalWeight == 0) return null  // no options or all weights zero
        val r = kotlin.random.Random.Default.nextDouble(totalWeight.toDouble())
        var cumulative = 0.0
        for (option in options) {
            cumulative += weightSelector(option)
            if (cumulative >= r) {
                return option
            }
        }
        return null  // should not happen if weights are positive
    }


}


