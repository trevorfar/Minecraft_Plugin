package com.trevorfarias.runic_overlord.fishing

import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin

class FishingLevel {
    object FishingDataKeys {
        lateinit var LEVEL       : NamespacedKey
        lateinit var XP          : NamespacedKey

        fun init(plugin: JavaPlugin) {
            LEVEL = NamespacedKey(plugin, "fishing_level")
            XP    = NamespacedKey(plugin, "fishing_xp")
        }
    }
    object FishingLeveling {

        /** XP needed to advance from `level` → `level + 1`. */
        fun xpNeeded(level: Int): Double {
            // simple quadratic curve – tweak to taste
            val n = level - 1
            return 75.0 + 3.5 * n * n         }

        /** Add XP, auto-levelling as needed, and return `true` if player levelled up. */
        fun addXp(player: Player, rawXp: Double): Boolean {
            val pdc       = player.persistentDataContainer
            var level     = pdc.get(FishingDataKeys.LEVEL, PersistentDataType.INTEGER) ?: 1
            var xp        = pdc.get(FishingDataKeys.XP,    PersistentDataType.DOUBLE)  ?: 0.0
            xp           += rawXp

            var levelled  = false
            while (xp >= xpNeeded(level)) {
                xp      -= xpNeeded(level)
                level   += 1
                levelled = true
            }

            pdc.set(FishingDataKeys.LEVEL, PersistentDataType.INTEGER, level)
            pdc.set(FishingDataKeys.XP,    PersistentDataType.DOUBLE,  xp)
            return levelled
        }

        fun level(player: Player) = player.persistentDataContainer
            .get(FishingDataKeys.LEVEL, PersistentDataType.INTEGER) ?: 1

        fun progress(player: Player): Pair<Double,Double> {
            val lvl = level(player)
            val xp  = player.persistentDataContainer.get(
                FishingDataKeys.XP, PersistentDataType.DOUBLE
            ) ?: 0.0
            return xp to xpNeeded(lvl)
        }
    }



}