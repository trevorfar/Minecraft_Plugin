package com.trevorfarias.runic_overlord.fishing

import com.trevorfarias.runic_overlord.fishing.FishingRewards.instantCatch
import com.trevorfarias.runic_overlord.gear.GearFactory
import com.trevorfarias.runic_overlord.util.Constants
import org.bukkit.Bukkit
import org.bukkit.entity.FishHook
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerFishEvent
import java.util.*

class CustomFishingGearListener : Listener {

    private val willowCooldown = mutableMapOf<UUID, Long>()
    private val WILLOW_COOLDOWN_TICKS = 50L
    private val plugin = Bukkit.getPluginManager().getPlugin("RunicOverlord")!!

    @EventHandler
    fun onPlayerFish(event: PlayerFishEvent) {
        val player = event.player
        val biome  = player.location.block.biome
        val mainId = GearFactory.getSpec(player.inventory.itemInMainHand)?.id
        val mainHand = player.inventory.itemInMainHand
        val offId = GearFactory.getSpec(player.inventory.itemInOffHand)?.id

        val quality = mainHand.itemMeta?.persistentDataContainer
            ?.get(Constants.GEAR_QUALITY_KEY, org.bukkit.persistence.PersistentDataType.INTEGER)
            ?: 100  // fallback if not present

        val spec = GearFactory.getSpec(mainHand)
        val baseChance = spec?.abilityBaseChance ?: 0.0
        val abilityChance = baseChance * (quality / 100.0)


        // Gillian’s Hook logic
        if (offId == "gillians_hook" && event.state == PlayerFishEvent.State.FISHING) {
            val hook = event.hook as? FishHook ?: return

            val oldMin = hook.minWaitTime
            val oldMax = hook.maxWaitTime

            // abilityChance is 0-100 (%). 10 % faster → keep 90 % of the wait.
            val speedPct = abilityChance.coerceAtMost(100.0)
            val factor   = 1.0 - (speedPct / 100.0)      // e.g. 10 → 0.90

            var newMin = (oldMin * factor).toInt()
            var newMax = (oldMax * factor).toInt()

            // Hard safety: 1–600 ticks and min < max
            newMin = newMin.coerceIn(1, 600)
            newMax = newMax.coerceIn(newMin + 1, 600)

            hook.maxWaitTime = newMax   // set max first
            hook.minWaitTime = newMin
            hook.applyLure   = false

            player.sendMessage("§b[Gillian] bite: §e$oldMin-$oldMax§7 → §a$newMin-$newMax ticks")
            Bukkit.getLogger().info("[Gillian] ${player.name}: $oldMin-$oldMax → $newMin-$newMax")
        }

        // Willow’s Rod logic
        val now = Bukkit.getCurrentTick()
        val lastUse = willowCooldown[player.uniqueId] ?: 0L
        val ready   = now - lastUse >= WILLOW_COOLDOWN_TICKS

        if (event.state == PlayerFishEvent.State.FISHING) {
            if (mainId == "willows_rod" && ready && Math.random() < (abilityChance / 100.0)) {
                willowCooldown[player.uniqueId] = now.toLong()
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    instantCatch(player, biome)
                    player.swingMainHand()
                })
            }
        }
    }
}