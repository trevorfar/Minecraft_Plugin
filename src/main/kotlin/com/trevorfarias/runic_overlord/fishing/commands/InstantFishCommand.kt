/* com/trevorfarias/runic_overlord/fishing/commands/InstantFishCommand.kt */
package com.trevorfarias.runic_overlord.fishing.commands

import com.trevorfarias.runic_overlord.fishing.*
import net.kyori.adventure.text.Component
import org.bukkit.Sound
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import kotlin.math.pow
import java.util.concurrent.ThreadLocalRandom

class InstantFishCommand : CommandExecutor {

    override fun onCommand(
        sender: CommandSender,
        cmd: Command,
        label: String,
        args: Array<out String>
    ): Boolean {

        val p = sender as? Player ?: return true
        val biome = p.location.block.biome

        /* --- reproduce giveFish() logic in a public helper --- */
        val spec = FishingConfig.randomFishForPlayer(p, biome) ?: return true
        var tier: TierSpec
        do { tier = FishingConfig.randomTier() } while (tier.id.equals("epic", true) && FishingLevel.FishingLeveling.level(p) < 10)

        val weight = spec.randomWeight(tier.minWeightPercent)
        val sell   = weight.pow(PriceModel.WEIGHT_EXP) *
                spec.baseValue *
                PriceModel.FACTOR *
                tier.multiplier

        val item = ItemStack(spec.material).apply {
            itemMeta = itemMeta!!.apply {
                setDisplayName("${tier.colour}${tier.pretty} ${spec.displayName} §7(${String.format("%.2f", weight)} kg)")
                lore = listOf(
                    "§8Tier Mult: ×${tier.multiplier}",
                    "§8Sell: §e$${"%,.2f".format(sell)}"
                )
                persistentDataContainer.apply {
                    set(FishingKeys.FISH_ID,     PersistentDataType.STRING,  spec.id)
                    set(FishingKeys.FISH_WEIGHT, PersistentDataType.DOUBLE,  weight)
                    set(FishingKeys.FISH_TIER,   PersistentDataType.STRING,  tier.id)
                }
            }
        }

        p.inventory.addItem(item)
        p.playSound(p.location, Sound.ENTITY_ITEM_PICKUP, 0.6f, 1.25f)
        p.sendMessage(Component.text("§aYou instantly reeled in a fish!"))
        return true
    }
}
