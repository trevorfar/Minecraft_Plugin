package com.trevorfarias.runic_overlord.fishing

import com.trevorfarias.runic_overlord.RunicOverlord
import com.trevorfarias.runic_overlord.fishing.model.*
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Biome
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import roll
import java.io.File
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.pow
import kotlin.math.max

/* ──────────────────────────  ECONOMY MODEL  ──────────────────────────── */
object PriceModel {
    /** Pull this up or down to inflate / deflate the whole fish market. */
    const val FACTOR      = 1.0

    /** 1.0 → linear, 0.0 → weight-agnostic, 0.60 picked to flatten extremes. */
    const val WEIGHT_EXP  = 0.60

    /** Mid-weight target price per tier. */
    val tierTarget = mapOf(
        "common"   to  100.0,
        "uncommon" to  350.0,
        "rare"     to 2_000.0,
        "epic"     to 15_000.0,
        "mythical" to 25_000.0
    )
}

/* ──────────────────────────  MAIN CONFIG  ────────────────────────────── */
object FishingConfig {

    /* ------------ fish + loot-tables parsed from YAML ------------------ */
    val fish   : MutableMap<String, FishSpec>   = mutableMapOf()
    private val tables : MutableMap<String, LootTableSpec> = mutableMapOf()

    /* ------------ tiers (Common / Uncommon …) -------------------------- */
    val tiers  : MutableMap<String, TierSpec>   = mutableMapOf()
    private   lateinit var weightedTierPool     : List<Pair<Double, TierSpec>>

    /* =================================================================== */
    /** Call once from onEnable() or via /rof reload. */
    fun load(plugin: JavaPlugin = RunicOverlord.instance) {
        fish.clear(); tables.clear(); tiers.clear()

        val file = File(plugin.dataFolder, "fishing.yml")
        if (!file.exists()) plugin.saveResource("fishing.yml", false)
        val root = YamlConfiguration.loadConfiguration(file)

        loadTiers(root)
        loadFish (root)
        loadTables(root)

        if ("default" !in tables) {
            tables["default"] = LootTableSpec(1, null, listOf(FishRewardSpec()))
        }
    }

    fun reload() = load()

    /* ─────────────────────── load Tiers ──────────────────────────────── */
    private fun loadTiers(root: YamlConfiguration) {
        root.getConfigurationSection("tiers")?.getKeys(false)?.forEach { id ->
            val s = root.getConfigurationSection("tiers.$id")!!
            tiers[id.lowercase()] = TierSpec(
                id           = id.lowercase(),
                colour       = s.getString("colour") ?: "§f",
                pretty       = s.getString("pretty") ?: id.replaceFirstChar { it.uppercase() },
                multiplier   = s.getDouble("multiplier", 1.0),
                chance       = s.getDouble("chance", 1.0),
                broadcast    = s.getBoolean("broadcast", false),
                minWeightPercent = s.getDouble("min-weight-percent", 0.0)
            )
        }

        /* pre-compute cumulative weights for randomTier() */
        var sum = 0.0
        weightedTierPool = tiers.values.map { t -> sum += t.chance; sum to t }
    }

    fun randomTier(): TierSpec {
        val r = ThreadLocalRandom.current().nextDouble(weightedTierPool.last().first)
        return weightedTierPool.first { r < it.first }.second
    }

    /* ─────────────────────── load Fish ───────────────────────────────── */
    private fun loadFish(root: YamlConfiguration) {
        root.getConfigurationSection("fish")?.getKeys(false)?.forEach { id ->

            val s = root.getConfigurationSection("fish.$id")!!
            val spec = FishSpec(
                id          = id,
                displayName = s.getString("display-name") ?: id,
                material    = Material.valueOf(s.getString("material")!!.uppercase()),
                minWeight   = s.getDouble("min-weight"),
                maxWeight   = s.getDouble("max-weight"),
                baseValue   = s.getDouble("base-value", -1.0),   // -1 → auto-derive
                biomes      = s.getStringList("biomes")
                    .mapNotNull { runCatching { Biome.valueOf(it) }.getOrNull() }
                    .toSet(),
                minLevel    = s.getInt("min-level", 1)
            )

            /* ── auto-derive baseValue if YAML left it blank ─────────── */
            if (spec.baseValue <= 0.0) {
                val tierId   = "common"               // assume baseline tier
                val tierMult = tiers[tierId]?.multiplier ?: 1.0
                val target   = PriceModel.tierTarget.getValue(tierId)
                val avgW = (spec.minWeight + spec.maxWeight) / 2.0
                val weightTerm = avgW.pow(PriceModel.WEIGHT_EXP)
                spec.baseValue = target / (PriceModel.FACTOR * tierMult * weightTerm)
            }


            fish[id] = spec
        }
    }

    /* ─────────────────────── load loot-tables ───────────────────────── */
    private fun loadTables(root: YamlConfiguration) {
        root.getConfigurationSection("tables")?.getKeys(false)?.forEach { key ->
            tables[key] = parseTable(key, root.getConfigurationSection("tables.$key")!!)
        }
    }

    /* ─────────────────────── API for listeners ──────────────────────── */
    fun tableForBiome(biome: Biome): LootTableSpec =
        tables[biome.name()] ?: tables.getValue("default")

    fun randomFishForPlayer(p: Player, biome: Biome): FishSpec? {
        val lvl  = FishingLevel.FishingLeveling.level(p)
        return fish.values
            .filter { it.minLevel <= lvl && (it.biomes.isEmpty() || biome in it.biomes) }
            .randomOrNull()
    }

    fun randomRewardFor(player: Player, table: LootTableSpec): RewardSpec {
        val lvl          = FishingLevel.FishingLeveling.level(player)
        val unlockedCats = RewardCategory.unlocked(lvl)

        val eligible = table.rewards.filter { r ->
            unlockedCats.contains(r.category ?: RewardCategory.JOURNEYMAN)
        }
        val safe = if (eligible.isNotEmpty()) eligible else table.rewards
        return table.copy(rewards = safe).roll()
    }

    /* ─────────────────────── YAML → LootTableSpec ───────────────────── */
    private fun parseTable(name: String, s: org.bukkit.configuration.ConfigurationSection): LootTableSpec {
        val inherit = s.getString("inherit")
        val parentRewards = inherit?.let { tables[it]?.rewards } ?: emptyList()

        val rewards = s.getList("rewards")?.mapNotNull { any ->
            @Suppress("UNCHECKED_CAST") val map = any as? Map<String, Any> ?: return@mapNotNull null

            val cat = (map["category"] as? String)
                ?.let { runCatching { RewardCategory.valueOf(it.uppercase()) }.getOrNull() }
                ?: RewardCategory.JOURNEYMAN
            val bc  = (map["broadcast"] as? Boolean) ?: false

            when ((map["type"] as String).uppercase()) {
                "FISH" -> FishRewardSpec(
                    weight = (map["weight"] as? Number)?.toDouble() ?: 1.0,
                    category = cat,
                    broadcast = bc
                )
                "RUNE" -> RuneRewardSpec(
                    runeId   = map["id"].toString(),
                    chance   = (map["chance"] as Number).toDouble(),
                    category = cat,
                    broadcast = bc
                )
                "VOUCHER" -> VoucherRewardSpec(
                    voucherId = map["id"].toString(),
                    chance    = (map["chance"] as Number).toDouble(),
                    category  = cat,
                    broadcast = bc
                )
                "ITEM" -> ItemRewardSpec(
                    material  = Material.valueOf(map["material"].toString().uppercase()),
                    name      = map["name"] as? String,
                    min       = (map["min"] as? Number)?.toInt() ?: 1,
                    max       = max((map["max"] as? Number)?.toInt() ?: 1, 1),
                    chance    = (map["chance"] as Number).toDouble(),
                    category  = cat,
                    broadcast = bc
                )
                "GEAR" -> GearRewardSpec(
                    gearId = map["id"].toString(),
                    chance = (map["chance"] as Number).toDouble(),
                    unidentified = map["unidentified"] as? Boolean ?: false
                )
                else -> null
            }
        } ?: emptyList()

        return LootTableSpec(
            rolls   = s.getInt("rolls", 1),
            inherit = inherit,
            rewards = parentRewards + rewards
        )
    }
}

/* ─────────────────────── NBT keys used by fishing ─────────────────── */
object FishingKeys {
    private val plugin: JavaPlugin get() = RunicOverlord.instance

    val FISH_ID     = NamespacedKey(plugin, "fish_id")
    val FISH_WEIGHT = NamespacedKey(plugin, "fish_weight")
    val FISH_TIER   = NamespacedKey(plugin, "fish_tier")
}
