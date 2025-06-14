package com.trevorfarias.runic_overlord.gear

import com.trevorfarias.runic_overlord.gear.GearFactory.getSpec
import com.trevorfarias.runic_overlord.gear.QualityScale.colour
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
    private val rng = ThreadLocalRandom.current()

    var locationTagPrettyNames: Map<String, String> = emptyMap()
    fun getAllSpecs() = specs.values.toList()
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
            val rarity = Rarity.valueOf(s.getString("rarity")!!.uppercase())
            val tier = Tier.valueOf(s.getString("tier")!!.uppercase())

            val material = Material.valueOf(s.getString("material")!!.uppercase())
            val namePlain = s.getString("display-name")!!
            val displayName = "${rarity.colour().toLegacy()}[${rarity.name}] §f$namePlain"

            val loreList = s.getStringList("lore")
            val slots = s.getStringList("equipment-slots")
                .mapNotNull { runCatching { EquipmentSlot.valueOf(it.uppercase()) }.getOrNull() }
                .toSet().ifEmpty { setOf(EquipmentSlot.HAND) }

            val locations = s.getStringList("locations")
            val abilityBaseChance = s.getDouble("ability-base-chance", 0.0)

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
                    else -> null
                }
            }

            val spec = GearSpec(
                id = id,
                material = material,
                displayName = displayName,
                stats = stats,
                loreBuilder = { pct, r ->
                    val abilityPct = abilityBaseChance * (pct / 100.0)
                    val coloured = "${r.colour().toLegacy()}${"%.1f".format(abilityPct)}"
                    val lore = loreList.map { line ->
                        line.replace("<rarity_colour>", r.colour().toLegacy())
                            .replace("<rarity_name>", r.name.lowercase().replaceFirstChar { it.titlecase() })
                            .replace("<quality>", pct.toString())
                            .replace("<ability_pct>", coloured)
                    }.toMutableList()
                    if (lore.none { it.contains("Quality:") })
                        lore += "§7Quality: ${r.colour().toLegacy()}${r.name} §8($pct%)"
                    lore
                },
                equipmentSlots = slots,
                locationTags = locations,
                rarity = rarity,
                tier = tier,
                abilityBaseChance = abilityBaseChance
            )
            specs[id] = spec
        }
        Bukkit.getLogger().info("[RunicOverlord] Loaded ${specs.size} gear specs.")
    }

    fun register(spec: GearSpec) {
        specs[spec.id] = spec
    }

    // ---- public builders --------------------------------------------------
    fun create(id: String, quality: Int = rollQuality()) =
        specs[id]?.let { buildStack(it, quality) }

    fun createUnidentified(id: String) = create(id, -1)



    // ---- core -------------------------------------------------------------
    private fun buildStack(spec: GearSpec, quality: Int): ItemStack {
        val item = ItemStack(spec.material)
        val meta = item.itemMeta!!

        meta.setDisplayName(spec.displayName)
        val rarity = QualityScale.rarityOf(quality)
        meta.lore = spec.loreBuilder(quality, rarity)

        with(meta.persistentDataContainer) {
            set(Constants.GEAR_ID_KEY, PersistentDataType.STRING, spec.id)
            set(Constants.GEAR_QUALITY_KEY, PersistentDataType.INTEGER, quality)
            set(Constants.GEAR_IDENTIFIED_KEY, PersistentDataType.INTEGER, if (quality >= 0) 1 else 0)
        }

        if (quality >= 0) spec.stats.forEach { it.apply(meta, quality) }
        else meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)

        item.itemMeta = meta
        return item
    }


    // ---- rarity-weighted roll --------------------------------------------
    fun rollQuality(): Int = rng.nextInt(1, 101)


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

    private fun net.kyori.adventure.text.format.TextColor.toLegacy() =
        when (this) {
            NamedTextColor.RED -> "§c"
            NamedTextColor.GRAY -> "§7"
            NamedTextColor.GREEN -> "§a"
            NamedTextColor.AQUA -> "§b"
            NamedTextColor.LIGHT_PURPLE -> "§d"
            NamedTextColor.GOLD -> "§6"
            else -> "§f"
        }
    fun markIdentified(item: ItemStack, quality: Int) {
        val spec  = getSpec(item) ?: return
        val meta  = item.itemMeta ?: return
        val q     = quality.coerceIn(1, 100)
        val rar   = QualityScale.rarityOf(q)          // ← new mapper

        // rebuild lore with new rarity
        meta.lore = spec.loreBuilder(q, rar)

        // update PDC flags
        with(Constants) {
            meta.persistentDataContainer.set(GEAR_QUALITY_KEY,   PersistentDataType.INTEGER, q)
            meta.persistentDataContainer.set(GEAR_IDENTIFIED_KEY,PersistentDataType.INTEGER, 1)
        }

        // apply stats now that quality is known
        spec.stats.forEach { it.apply(meta, q) }
        meta.removeItemFlags(ItemFlag.HIDE_ATTRIBUTES)

        item.itemMeta = meta
    }

}

    /**
     * Re-writes an *unidentified* gear ItemStack with its rolled quality:
     *  • sets the new PDC values<br>
     *  • rebuilds lore via the spec’s loreBuilder<br>
     *  • applies every StatModifier<br>
     */




