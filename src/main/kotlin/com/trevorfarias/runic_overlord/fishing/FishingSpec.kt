package com.trevorfarias.runic_overlord.fishing

import org.bukkit.Material
import org.bukkit.block.Biome
import java.util.concurrent.ThreadLocalRandom

data class FishSpec(
    val id: String,
    val displayName: String,
    val material: Material,
    val minWeight: Double,
    val maxWeight: Double,
    var baseValue: Double,
    val biomes: Set<Biome>,
    val minLevel: Int = 1
) {

    fun randomWeight(minPercent: Double = 0.0): Double {
        val clamped = minPercent.coerceIn(0.0, 1.0)
        val low = minWeight + (maxWeight - minWeight) * clamped
        val high = maxWeight
        return ThreadLocalRandom.current().nextDouble(low, high)
            .let { "%.2f".format(it).toDouble() }      // keep 2-dp
    }
}

data class TierSpec(
    val id: String,
    val colour: String,
    val pretty: String,
    val multiplier: Double,
    val chance: Double,
    val broadcast: Boolean = false,
    val minWeightPercent: Double = 0.0      // NEW

)
sealed interface RewardSpec {
    val category: RewardCategory?
    val broadcast: Boolean
}
data class FishRewardSpec(
    val weight: Double = 1.0,
    override val category: RewardCategory? = null,
    override val broadcast: Boolean = false
) : RewardSpec

data class RuneRewardSpec(
    val runeId: String,
    val chance: Double,
    override val category: RewardCategory? = null,
    override val broadcast: Boolean = false
) : RewardSpec
data class VoucherRewardSpec(
    val voucherId: String,
    val chance: Double,
    override val category: RewardCategory?,
    override val broadcast: Boolean = false
    )
    : RewardSpec
data class ItemRewardSpec(
    val material: Material,
    val name: String?,
    val min: Int,
    val max: Int,
    val chance: Double,
    override val category: RewardCategory?,
    override val broadcast: Boolean = false
) : RewardSpec

data class LootTableSpec(
    val rolls: Int = 1,
    val inherit: String? = null,
    val rewards: List<RewardSpec>
)
