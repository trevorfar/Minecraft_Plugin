import com.trevorfarias.runic_overlord.fishing.*
import org.bukkit.Bukkit
import java.util.concurrent.ThreadLocalRandom

fun LootTableSpec.roll(): RewardSpec {
    // Split rewards that have explicit "chance" (probability) vs weight-based selection
    val probabilistic = rewards.filter {
        when (it) {
            is RuneRewardSpec, is VoucherRewardSpec, is ItemRewardSpec -> true
            else -> false
        }
    }
    probabilistic.forEach {
        val chance = when (it) {
            is RuneRewardSpec   -> it.chance
            is VoucherRewardSpec -> it.chance
            is ItemRewardSpec   -> it.chance
            else                -> 0.0
        }
        Bukkit.getLogger().info("[DEBUG] Roll table=${this}  reward=$it  chance=$chance")

        if (ThreadLocalRandom.current().nextDouble() < chance) return it

    }

    // If nothing procced, pick fish (or the first reward) as fallback

    val fishRewards = rewards.filterIsInstance<FishRewardSpec>()
    return if (fishRewards.isNotEmpty()) fishRewards.random() else rewards.first()
}
