package com.trevorfarias.runic_overlord.gear

import com.trevorfarias.runic_overlord.util.Constants
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.inventory.EquipmentSlot
import java.util.concurrent.ThreadLocalRandom


object GearFactory {

    private val specs = mutableMapOf<String, GearSpec>()
    private val rng   = ThreadLocalRandom.current()

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

    fun initDefaults() {
        register(
            GearSpec(
                id = "silver_sword",
                material = Material.IRON_SWORD,
                displayName = "§bSilver Sword",

                stats = listOf(
                    AttributeStat(
                        attribute = Attribute.ATTACK_DAMAGE,   // ← correct enum
                        slot      = EquipmentSlot.HAND,
                        min       = 5.0,
                        max       = 20.0
                    )
                ),

                loreBuilder = { pct, rarity ->
                    if (pct < 0) {
                        listOf("§7Quality: §oUnidentified")
                    } else {
                        listOf("§7Quality: ${rarity.colour.toLegacy()}${rarity.display} §f($pct%)")
                    }
                }
            )
        )


        // register more gear here …
        // register(GearSpec(id = "bronze_axe", …)
        // register(GearSpec(id = "iron_helmet", …)

        Bukkit.getLogger().info("[RunicOverlord] Registered ${specs.size} gear types.")
    }
}
