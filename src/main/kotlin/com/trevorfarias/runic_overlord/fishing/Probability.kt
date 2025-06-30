package com.trevorfarias.runic_overlord.fishing

import com.trevorfarias.runic_overlord.gear.Rarity
import com.trevorfarias.runic_overlord.gear.Tier
import org.bukkit.Bukkit


class Probability(val allRewards: List<RewardSpec>) { // Returns an Item, where item is either a GearRewardSpec, FishRewardSpec or VoucherRewardSpec

    enum class ParentCategory(val weight: Int) {
        FISH(90),
        ITEM(5),
        VOUCHER(5)

    }


    fun roll(): RewardSpec? {
        val parent = pickWeightedRandom(ParentCategory.values().toList()) { it.weight } ?: return null

        val categoryFiltered = allRewards.filter {
            when (parent) {
                ParentCategory.FISH    -> it is FishRewardSpec
                ParentCategory.ITEM    -> it is GearRewardSpec
                ParentCategory.VOUCHER -> it is VoucherRewardSpec
            }
        }

        val rarity      = pickWeightedRandom(Rarity.values().toList()) { it.weight } ?: return null
        val startTier   = pickWeightedRandom(Tier.values().toList())   { it.weight } ?: return null
        val tiers       = Tier.values()                        // [I, II, III] (in declaration order)
        val startIdx    = tiers.indexOf(startTier)

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

        return roll()
    }


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


