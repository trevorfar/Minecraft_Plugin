package com.trevorfarias.runic_overlord.fishing

import com.trevorfarias.runic_overlord.RunicOverlord
import com.trevorfarias.runic_overlord.gear.GearFactory
import com.trevorfarias.runic_overlord.gear.GearSpec
import com.trevorfarias.runic_overlord.gear.Rarity
import com.trevorfarias.runic_overlord.gear.Tier
import com.trevorfarias.runic_overlord.voucher.VoucherFactory
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Biome
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.pow
import kotlin.math.max

/* ──────────────────────────  ECONOMY MODEL  ──────────────────────────── */
object PriceModel {
    /** Pull this up or down to inflate / deflate the whole fish market. */
    const val FACTOR      = 2.0

    /** 1.0 → linear, 0.0 → weight-agnostic, 0.60 picked to flatten extremes. */
    const val WEIGHT_EXP  = 0.60

    /** Mid-weight target price per tier. */
    val tierTarget = mapOf(
        "common"   to  100.0,
        "uncommon" to  350.0,
        "rare"     to 2_000.0,
        "epic"     to 15_000.0,
        "legendary" to 25_000.0
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
            tables["default"] = LootTableSpec(1, null, listOf(
                FishRewardSpec(
                    weight = 1.0,
                    rarity = Rarity.COMMON,
                    tier = Tier.I,
                    category = RewardCategory.JOURNEYMAN
                )
            ))        }
    }

    fun reload() = load()

    fun allRewardSpecs(): List<RewardSpec> {
        val fishRewards = fish.map { (_, spec) ->
            FishRewardSpec(
                rarity = spec.rarity,
                tier = spec.tier,
                category = RewardCategory.forLevel(spec.minLevel)
            )
        }

        val gearRewards = GearFactory.getAllSpecs().map { spec ->
            GearRewardSpec(
                gearId       = spec.id,
                unidentified = false,
                rarity       = spec.rarity,
                tier         = spec.tier,
            )
        }

        val voucherRewards = VoucherFactory.allSpecs().map { s ->
            VoucherRewardSpec(
                voucherId = s.id,
                category  = null,
                broadcast = false,
                rarity    = s.rarity,
                tier      = s.tier
            )
        }


        return tables.values.flatMap { it.rewards } + fishRewards + gearRewards + voucherRewards
    }

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
                minLevel    = s.getInt("min-level", 1),
                rarity = Rarity.valueOf(s.getString("rarity")!!.uppercase()),
                tier = Tier.valueOf(s.getString("tier")!!.uppercase()),
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

    fun randomFishForPlayer(
        p: Player,
        biome: Biome,
        rarity: Rarity? = null,
        tier:   Tier?   = null
    ): FishSpec? {
        val lvl = FishingLevel.FishingLeveling.level(p)

        return fish.values
            .filter { it.minLevel <= lvl && (it.biomes.isEmpty() || biome in it.biomes) }
            .filter { rarity == null || it.rarity == rarity }
            .filter { tier   == null || it.tier   == tier }
            .randomOrNull()
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
            val rarity = (map["rarity"] as? String)?.let { Rarity.valueOf(it.uppercase()) } ?: Rarity.COMMON
            val tier   = (map["tier"]   as? String)?.let { Tier.valueOf(it.uppercase()) }   ?: Tier.I

            when ((map["type"] as String).uppercase()) {
                "FISH" -> FishRewardSpec(
                    weight = (map["weight"] as? Number)?.toDouble() ?: 1.0,
                    category = cat,
                    broadcast = bc,
                    rarity = rarity,
                    tier = tier
                )
                "RUNE" -> RuneRewardSpec(
                    runeId   = map["id"].toString(),
                    category = cat,
                    broadcast = bc,
                    rarity = rarity,
                    tier = tier
                )
                "VOUCHER" -> VoucherRewardSpec(
                    voucherId = map["id"].toString(),
                    category  = cat,
                    broadcast = bc,
                    rarity = rarity,
                    tier = tier
                )
                "ITEM" -> ItemRewardSpec(
                    material  = Material.valueOf(map["material"].toString().uppercase()),
                    name      = map["name"] as? String,
                    min       = (map["min"] as? Number)?.toInt() ?: 1,
                    max       = max((map["max"] as? Number)?.toInt() ?: 1, 1),
                    category  = cat,
                    broadcast = bc,
                    rarity = rarity,
                    tier = tier
                )
                "GEAR" -> GearRewardSpec(
                    gearId = map["id"].toString(),
                    unidentified = map["unidentified"] as? Boolean ?: false,
                    rarity = rarity,
                    tier = tier
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
