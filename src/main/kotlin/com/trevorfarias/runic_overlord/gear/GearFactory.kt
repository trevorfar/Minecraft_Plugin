package com.trevorfarias.runic_overlord.gear

import com.trevorfarias.runic_overlord.fishing.FishingConfig.load
import com.trevorfarias.runic_overlord.util.Constants
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.concurrent.ThreadLocalRandom


object GearFactory {

    private val specs = mutableMapOf<String, GearSpec>()
    private val rng   = ThreadLocalRandom.current()

    var locationTagPrettyNames: Map<String, String> = emptyMap()
    fun getAllSpecs(): List<GearSpec> = specs.values.toList()
    fun reloadFromYML(plugin: JavaPlugin) {
        specs.clear()
        val file = File(plugin.dataFolder, "gear.yml")
        if (!file.exists()) plugin.saveResource("gear.yml", false)
        val root = YamlConfiguration.loadConfiguration(file)
        val section = root.getConfigurationSection("gear") ?: return



        locationTagPrettyNames = root.getConfigurationSection("location-tags")
            ?.getKeys(false)
            ?.associateWith { key -> root.getString("location-tags.$key")!! }
            ?: emptyMap()

        for (id in section.getKeys(false)) {
            val s = section.getConfigurationSection(id) ?: continue
            val rarityKey = s.getString("rarity") ?: "COMMON"
            val rarity = Rarity.valueOf(rarityKey.uppercase())
            val displayName = "${rarity.colour.toLegacy()}[${rarity.display}] §f${s.getString("display-name")}"
            val material = Material.valueOf(s.getString("material")!!.uppercase())
            val loreList = s.getStringList("lore")
            val slots = s.getStringList("equipment-slots")
                .mapNotNull { runCatching { EquipmentSlot.valueOf(it.uppercase()) }.getOrNull() }
                .toSet().ifEmpty { setOf(EquipmentSlot.HAND) }

            val locations = s.getStringList("locations") // e.g., ["endcity", "altar"]
            val abilityBaseChance = s.getDouble("ability-base-chance", 0.0)

            // --- parse stats ---
            val stats = (s.getList("stats") ?: emptyList()).mapNotNull { raw ->
                val m = raw as? Map<*, *> ?: return@mapNotNull null
                when (m["type"]) {
                    "attribute" -> AttributeStat(
                        attribute = Attribute.valueOf(m["attribute"].toString().uppercase()),
                        slot = EquipmentSlot.valueOf(m["slot"].toString().uppercase()),
                        min = (m["min"] as Number).toDouble(),
                        max = (m["max"] as Number).toDouble()
                    )
                    "unbreakable" -> UnbreakableStat()
                    // Add more stat types here...
                    else -> null
                }
            }

            // --- loreBuilder replaces <rarity_colour>, <rarity_name>, <quality> ---
            val spec = GearSpec(
                id = id,
                material = material,
                displayName = displayName,
                stats = stats,
                loreBuilder = { pct, rarity ->
                    val abilityPct = abilityBaseChance * (pct / 100.0)
                    val coloredPct = "${rarity.colour.toLegacy()}${"%.1f".format(abilityPct)}"
                    val newLore = loreList.map { line ->
                        line.replace("<rarity_colour>", rarity.colour.toLegacy())
                            .replace("<rarity_name>", rarity.display)
                            .replace("<quality>", pct.toString())
                            .replace("<ability_pct", coloredPct)
                    }.toMutableList()

                    if (newLore.none { it.contains("Quality") }) {
                        newLore += "§7Quality: ${rarity.colour.toLegacy()}${rarity.display} §8($pct%)"
                    }
                    newLore
                },
                equipmentSlots = slots,
                locationTags = locations,
                rarity = rarity,
                abilityBaseChance = abilityBaseChance
            )
            register(spec)
        }
        Bukkit.getLogger().info("[RunicOverlord] Loaded ${specs.size} gear specs from YAML.")
    }

    fun register(spec: GearSpec) { specs[spec.id] = spec }

    // ---- public builders --------------------------------------------------
    fun create(id: String, quality: Int = rollQuality()): ItemStack? =
        specs[id]?.let { buildStack(it, quality) }

    fun createUnidentified(id: String) =
        create(id, -1)
    // -1 = hidden


    fun TextColor.toLegacy(): String = when (this) {
        NamedTextColor.RED           -> "§c"
        NamedTextColor.GRAY          -> "§7"
        NamedTextColor.GREEN         -> "§a"
        NamedTextColor.AQUA          -> "§b"
        NamedTextColor.LIGHT_PURPLE  -> "§d"
        NamedTextColor.GOLD          -> "§6"
        else                         -> "§f"   // fallback white
    }

    // ---- core -------------------------------------------------------------
    private fun buildStack(spec: GearSpec, quality: Int): ItemStack {
        val item = ItemStack(spec.material)
        val meta = item.itemMeta!!

        meta.setDisplayName(spec.displayName)
        val rarity = Rarity.of(quality.coerceIn(1, 100))
        meta.lore = spec.loreBuilder(quality, rarity)

        val pdc = meta.persistentDataContainer
        with(Constants) {
            pdc.set(GEAR_ID_KEY, PersistentDataType.STRING, spec.id)
            pdc.set(GEAR_QUALITY_KEY, PersistentDataType.INTEGER, quality)
            pdc.set(GEAR_IDENTIFIED_KEY, PersistentDataType.INTEGER, if (quality >= 0) 1 else 0)
        }

        if (quality >= 0) {
            spec.stats.forEach { it.apply(meta, quality) }
        } else {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
        }

        item.setItemMeta(meta)
        return item
    }



    // ---- rarity-weighted roll --------------------------------------------
    fun rollQuality(): Int {
        val rand = rng.nextDouble()
        var acc = 0.0
        for (tier in Rarity.entries) {
            acc += tier.weight
            if (rand <= acc) {
                return rng.nextInt(tier.range.first, tier.range.last + 1)
            }
        }
        return 1
    }

    fun getSpec(item: ItemStack?): GearSpec? {
        if (item == null) return null
        val id = item.itemMeta
            ?.persistentDataContainer
            ?.get(Constants.GEAR_ID_KEY, PersistentDataType.STRING)
            ?: return null
        return specs[id]
    }

    /** Returns quality percentage, or null if the stack isn’t our gear. */
    fun getQuality(item: ItemStack?): Int? =
        item?.itemMeta
            ?.persistentDataContainer
            ?.get(Constants.GEAR_QUALITY_KEY, PersistentDataType.INTEGER)

    /**
     * Re-writes an *unidentified* gear ItemStack with its rolled quality:
     *  • sets the new PDC values<br>
     *  • rebuilds lore via the spec’s loreBuilder<br>
     *  • applies every StatModifier<br>
     */
    fun markIdentified(item: ItemStack, quality: Int) {
        val spec = getSpec(item) ?: return
        val meta = item.itemMeta ?: return
        val rarity = Rarity.of(quality.coerceIn(1, 100))

        // overwrite lore
        meta.lore = spec.loreBuilder(quality, rarity)

        // update PDC flags
        with(Constants) {
            meta.persistentDataContainer.set(GEAR_QUALITY_KEY, PersistentDataType.INTEGER, quality)
            meta.persistentDataContainer.set(GEAR_IDENTIFIED_KEY, PersistentDataType.INTEGER, 1)
        }

        // apply stats now that quality is known
        spec.stats.forEach { it.apply(meta, quality) }
        meta.removeItemFlags(ItemFlag.HIDE_ATTRIBUTES)

        item.setItemMeta(meta)
    }
}
