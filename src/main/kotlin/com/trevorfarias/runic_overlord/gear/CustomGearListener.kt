package com.trevorfarias.runic_overlord.gear

import com.trevorfarias.runic_overlord.fishing.FishingRewards
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.event.player.PlayerFishEvent.State.*
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.concurrent.ThreadLocalRandom

object CustomGearListener : Listener {
    private val rng = ThreadLocalRandom.current()

    fun simulateEffect(gearId: String, item: ItemStack, target: Player, e: EntityDamageByEntityEvent) {
        when (gearId) {
            "sacrificial_dagger" -> {
                val trueDamage = e.finalDamage * 0.30
                target.damage(trueDamage)
                target.sendMessage("§c[TEST] Took §f$trueDamage §ctrue damage from sacrificial dagger.")
            }
            "dwarven_hammer" -> {
                target.damage(8.0)
                target.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.NAUSEA, 40, 1))
                target.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 20, 255))
                target.sendMessage("§e[TEST] Dwarven hammer stunned and disoriented you.")
            }
            else -> {
                target.sendMessage("§7[TEST] No simulation logic for gear: $gearId")
            }
        }
    }
    // ───────────── swords & melee ──────────────────────────────
    @EventHandler
    fun onHit(e: EntityDamageByEntityEvent) {
        val p = e.damager as? Player ?: return
        val spec = GearFactory.getSpec(p.inventory.itemInMainHand) ?: return
        val q = GearFactory.getQuality(p.inventory.itemInMainHand) ?: 0
        val roll = rng.nextDouble(100.0)
        val target = e.entity as? LivingEntity ?: return

        when (spec.id) {
            "frostbrand" -> tryChance(spec, q, roll) {
                e.entity.freezeTicks = 100        // 5 s slowness
                target.addPotionEffect(
                    PotionEffect(PotionEffectType.SLOWNESS, 50 /*5 s*/, 1, false, true, true)
                )
                p.playSound(p.location, Sound.BLOCK_GLASS_BREAK, 1f, 1.2f)
            }
            "steel_sabre" -> tryChance(spec, q, roll) {
                e.damage *= 1.05                  // flat 5 % crit bonus
            }
            "tempered_bronze_blade" -> tryChance(spec, q, roll) {
                e.entity.freezeTicks = 40         // tiny slow “bash”
                p.playSound(p.location, Sound.BLOCK_GLASS_BREAK, 1f, 1.2f)
                target.addPotionEffect(
                    PotionEffect(PotionEffectType.SLOWNESS, 10 /*2 s*/, 0, false, true, true)
                )
            }
            "nickel_longknife" -> tryChance(spec, q, roll) {
                e.entity.lastDamageCause?.damage = e.finalDamage + 1.5
            }
            "sanguine_edge" -> tryChance(spec, q, roll) {

                val heal = (e.finalDamage * 0.30).coerceAtMost(6.0)

                Bukkit.getLogger().info("HEALED $heal ${e.finalDamage}")
                p.health = (p.health + heal).coerceAtMost(p.maxHealth)
                p.playSound(p.location, Sound.ENTITY_PLAYER_BURP, 1f, 1.8f)
            }

            "sacrificial_dagger" -> tryChance(spec, q, roll) {
                val partialTrueDamage = e.finalDamage * 0.30
                (e.entity as? LivingEntity)?.damage(partialTrueDamage, p) // bypasses armor
                p.playSound(p.location, Sound.ENTITY_WITCH_CELEBRATE, 1f, 1.2f)
            }

            "dwarven_hammer" -> tryChance(spec, q, roll){
                if (target is Player) {
                    // Override default mace knockback / falling damage logic
                    e.damage = 9.0
                    e.isCancelled = false

                    // Apply stun and nausea
                    target.addPotionEffect(PotionEffect(PotionEffectType.NAUSEA, 40, 1, false, true, true)) // 2s nausea
                    target.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, 20, 255, false, false, false)) // 1s stun

                    p.playSound(p.location, Sound.BLOCK_ANVIL_HIT, 1f, 0.6f)
                }
            }

        }


    }

    // ───────────── helper ─────────────────────────────────────
    private inline fun tryChance(spec: GearSpec, quality: Int, roll: Double, block: () -> Unit) {
        val pct = spec.abilityBaseChance * (quality / 100.0)
        if (roll < pct) block()
    }
}