/* src/main/kotlin/com/trevorfarias/runic_overlord/gear/GearFactory.kt */
package com.trevorfarias.runic_overlord.gear
import com.trevorfarias.runic_overlord.util.Constants
import org.bukkit.Bukkit
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

object GearFactory {
    private val specs = mutableMapOf<String, GearSpec>()
    private val random = java.util.concurrent.ThreadLocalRandom.current()

    // --- registration ------------------------------------------------------
    fun register(spec: GearSpec) { specs[spec.id] = spec }

    // call from PluginManager.initialize()
    fun initDefaults() {
        register(
            GearSpec(
                id = "silver_sword",
                material = Material.IRON_SWORD,
                minDamage = 5.0,
                maxDamage = 20.0,
                displayName = "§bSilver Sword",
                tierLore = { q ->
                    listOf("§7Quality: §f${if (q < 0) "§oUnidentified" else "$q%"}")
                }
            )
        )
        Bukkit.getLogger().info("[RunicOverlord] Registered ${specs.size} custom gear types.")
    }

    // --- crafting / spawning ----------------------------------------------
    fun createUnidentified(id: String): ItemStack = buildItem(id, quality = -1)
    fun createWithRandomQuality(id: String): ItemStack =
        buildItem(id, quality = rollQuality())

    private fun buildItem(id: String, quality: Int): ItemStack {
        val spec = specs.getValue(id)
        val item = ItemStack(spec.material)
        val meta = item.itemMeta!!

        meta.setDisplayName(spec.displayName)
        meta.lore = spec.tierLore(quality)

        val pdc = meta.persistentDataContainer
        pdc.set(Constants.GEAR_ID_KEY, PersistentDataType.STRING, id)
        pdc.set(Constants.GEAR_QUALITY_KEY, PersistentDataType.INTEGER, quality)
        pdc.set(Constants.GEAR_IDENTIFIED_KEY, PersistentDataType.INTEGER, if (quality >= 0) 1 else 0)

        if (quality >= 0) meta.attributeModifiers = spec.buildAttrMods(quality).toMutableMultimap()

        item.setItemMeta(meta)
        return item
    }

    // --------------- helpers ----------------------------------------------
    fun getSpec(item: ItemStack): GearSpec? =
        item.itemMeta?.persistentDataContainer?.get(Constants.GEAR_ID_KEY, PersistentDataType.STRING)
            ?.let { specs[it] }

    fun getQuality(item: ItemStack): Int? =
        item.itemMeta?.persistentDataContainer?.get(Constants.GEAR_QUALITY_KEY, PersistentDataType.INTEGER)

    fun markIdentified(item: ItemStack, quality: Int) {
        val meta = item.itemMeta ?: return
        val spec = getSpec(item) ?: return

        val pdc = meta.persistentDataContainer
        pdc.set(Constants.GEAR_IDENTIFIED_KEY, PersistentDataType.INTEGER, 1)
        pdc.set(Constants.GEAR_QUALITY_KEY, PersistentDataType.INTEGER, quality)
        meta.lore = spec.tierLore(quality)
        meta.attributeModifiers = spec.buildAttrMods(quality).toMutableMultimap()

        item.setItemMeta(meta)
    }

    /** simple rarity curve: common 60 %, rare 30 %, epic 9 %, legendary 1 % */
    private fun rollQuality(): Int =
        when (val roll = random.nextDouble()) {
            in 0.0..0.60  -> random.nextInt(0, 51)     // 0-50
            in 0.60..0.90 -> random.nextInt(51, 81)    // 51-80
            in 0.90..0.99 -> random.nextInt(81, 96)    // 81-95
            else          -> random.nextInt(96, 101)   // 96-100
        }
}
