package com.trevorfarias.runic_overlord.fishing

import com.trevorfarias.runic_overlord.RunicOverlord
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Biome
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.max

object FishingConfig {

    /** All known fish specs keyed by id (e.g. `"salmon"`) */
    val fish: MutableMap<String, FishSpec> = mutableMapOf()

    /** Loot-tables keyed by name or biome (e.g. `"default"`, `"DEEP_OCEAN"`) */
    private val tables: MutableMap<String, LootTableSpec> = mutableMapOf()

    /** Lazily call from onEnable() once. */
    fun load(plugin: JavaPlugin = RunicOverlord.instance) {
        fish.clear()
        tables.clear()
        tiers.clear()

        // ── ensure file exists ───────────────────────────────────────────────
        val file = File(plugin.dataFolder, "fishing.yml")
        if (!file.exists()) plugin.saveResource("fishing.yml", false)
        val root = YamlConfiguration.loadConfiguration(file)

        loadTiers(root)
        // ── fish ----------------------------------------------------------------
        root.getConfigurationSection("fish")?.getKeys(false)?.forEach { id ->
            val s = root.getConfigurationSection("fish.$id")!!
            fish[id] = FishSpec(
                id = id,
                displayName = s.getString("display-name") ?: id,
                material = Material.valueOf(s.getString("material")!!.uppercase()),
                minWeight = s.getDouble("min-weight", 0.1),
                maxWeight = s.getDouble("max-weight", 1.0),
                baseValue = s.getDouble("base-value", 1.0),
                biomes = s.getStringList("biomes").mapNotNull { runCatching { Biome.valueOf(it) }.getOrNull() }.toSet()
            )
        }

        // ── loot-tables ---------------------------------------------------------
        root.getConfigurationSection("tables")?.getKeys(false)?.forEach { key ->
            tables[key] = parseTable(key, root.getConfigurationSection("tables.$key")!!)
        }

        // always have a fallback
        if ("default" !in tables) {
            tables["default"] = LootTableSpec(rolls = 1, inherit = null, rewards = listOf(FishRewardSpec()))
        }

        Bukkit.getLogger().info("[RunicOverlord] Loaded ${fish.size} fish + ${tables.size} fishing tables")
    }

    /** Reload command handler convenience */
    fun reload() = load()

    // inside object FishingConfig --------------------------------------
    val tiers: MutableMap<String, TierSpec> = mutableMapOf()
    private lateinit var weightedTierPool: List<Pair<Double, TierSpec>>

    private fun loadTiers(root: YamlConfiguration) {
        tiers.clear()
        root.getConfigurationSection("tiers")?.getKeys(false)?.forEach { id ->
            val s = root.getConfigurationSection("tiers.$id")!!
            tiers[id] = TierSpec(
                id        = id,
                colour    = s.getString("colour") ?: "§f",
                pretty    = s.getString("pretty") ?: id.capitalize(),
                multiplier= s.getDouble("multiplier", 1.0),
                chance    = s.getDouble("chance", 1.0),
                broadcast = s.getBoolean("broadcast", false),
                minWeightPercent = s.getDouble("min-weight-percent", 0.0)  // NEW

            )
        }
        // Pre-compute cumulative weights for O(log n) draw
        val cum = mutableListOf<Pair<Double, TierSpec>>()
        var sum = 0.0
        for (t in tiers.values) {
            sum += t.chance
            cum += sum to t
        }
        weightedTierPool = cum
    }

    fun randomTier(): TierSpec {
        val r = ThreadLocalRandom.current().nextDouble(weightedTierPool.last().first)
        return weightedTierPool.first { r < it.first }.second
    }


    // ── API for your listeners ────────────────────────────────────────────────
    fun tableForBiome(biome: Biome): LootTableSpec =
        tables[biome.name()] ?: tables["default"]!!

    fun randomFishForBiome(biome: Any): FishSpec? {
        val pool = fish.values.filter { it.biomes.isEmpty() || biome in it.biomes }
        return if (pool.isEmpty()) null else pool.random()
    }

    // ── helpers ───────────────────────────────────────────────────────────────
    private fun parseTable(name: String, s: org.bukkit.configuration.ConfigurationSection): LootTableSpec {
        val inherit = s.getString("inherit")
        val parentRewards = inherit?.let { tables[it]?.rewards } ?: emptyList()

        val rewards = s.getList("rewards")?.mapNotNull { any ->
            @Suppress("UNCHECKED_CAST") val map = any as? Map<String, Any> ?: return@mapNotNull null
            when ((map["type"] as String).uppercase()) {
                "FISH" -> FishRewardSpec((map["weight"] as? Number)?.toDouble() ?: 1.0)
                "RUNE" -> RuneRewardSpec(
                    runeId = map["id"].toString(),
                    chance = (map["chance"] as Number).toDouble()
                )
                "VOUCHER" -> VoucherRewardSpec(
                    voucherId = map["id"].toString(),
                    chance = (map["chance"] as Number).toDouble()
                )
                "ITEM" -> ItemRewardSpec(
                    material = Material.valueOf(map["material"].toString().uppercase()),
                    name = map["name"] as? String,
                    min = (map["min"] as? Number)?.toInt() ?: 1,
                    max = max((map["max"] as? Number)?.toInt() ?: 1, 1),
                    chance = (map["chance"] as Number).toDouble()
                )
                else -> null
            }
        } ?: emptyList()

        return LootTableSpec(
            rolls = s.getInt("rolls", 1),
            inherit = inherit,
            rewards = parentRewards + rewards
        )
    }
}

/** NamespacedKeys for fish NBT tags */
object FishingKeys {
    private val plugin: JavaPlugin get() = RunicOverlord.instance
    val FISH_ID     = NamespacedKey(plugin, "fish_id")
    val FISH_WEIGHT = NamespacedKey(plugin, "fish_weight")
    val FISH_TIER   = NamespacedKey(plugin, "fish_tier")

}
