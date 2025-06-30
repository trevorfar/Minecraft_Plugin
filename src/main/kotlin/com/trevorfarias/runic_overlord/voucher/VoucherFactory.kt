package com.trevorfarias.runic_overlord.voucher

import com.trevorfarias.runic_overlord.RunicOverlord
import com.trevorfarias.runic_overlord.gear.GearSpec
import com.trevorfarias.runic_overlord.gear.Rarity
import com.trevorfarias.runic_overlord.gear.Tier
import com.trevorfarias.runic_overlord.runes.RuneApplyListener
import com.trevorfarias.runic_overlord.runes.RuneFactory
import com.trevorfarias.runic_overlord.runes.RuneRemoverGuiListener
import com.trevorfarias.runic_overlord.util.Constants
import org.bukkit.*
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.concurrent.ThreadLocalRandom

object RuneGUI {
    fun openRuneRemovalGUI(player: Player, armor: ItemStack, runeIds: List<String>) {
        val size = ((runeIds.size + 8) / 9) * 9
        val gui = Bukkit.createInventory(null, size, "§cRemove a Rune")

        runeIds.forEachIndexed { i, id ->
            val spec = RuneFactory.getRuneSpecById(id) ?: return@forEachIndexed
            val item = ItemStack(spec.material)
            val meta = item.itemMeta ?: return@forEachIndexed
            meta.setDisplayName("§cRemove: ${spec.displayName}")
            item.itemMeta = meta
            gui.setItem(i, item)
        }

        Bukkit.getScheduler().runTask(RunicOverlord.instance, Runnable {
            player.setItemOnCursor(ItemStack(Material.AIR))
            player.openInventory(gui)
        })
    }
}

/* ───────────────── data classes ───────────────── */

data class VoucherSpec(
    val id: String,
    val displayName: String,
    val material: Material,
    val lore: List<String>,
    val rightClickable: Boolean = true,
    val rewardDef: RewardDef? = null,
    val customAction: VoucherAction? = null,
    val rarity: Rarity,
    val tier: Tier
)
typealias VoucherAction =
            (player: Player,
             target: ItemStack,
             cursor: ItemStack,
             e: InventoryClickEvent?) -> Boolean   //  ← “?” here

/** Declarative rewards loaded from YAML. */
sealed interface RewardDef {
    data class GiveItem(val item: Material, val amount: Int) : RewardDef
    data class GiveXP(val amount: Int)                       : RewardDef
    object TeleportSpawn                                      : RewardDef
}

/* ───────────────── factory ───────────────── */

object VoucherFactory {

    private val registry = mutableMapOf<String, VoucherSpec>()
    fun allIds(): Collection<String> = registry.keys

    private val specs = mutableMapOf<String, VoucherSpec>()
    private val rng   = ThreadLocalRandom.current()

    fun reload(plugin: JavaPlugin = RunicOverlord.instance) = loadFromYml(plugin)

    fun loadFromYml(plugin: JavaPlugin) {
        registry.clear()

        val file = File(plugin.dataFolder, "vouchers.yml")
        if (!file.exists()) plugin.saveResource("vouchers.yml", false)
        val root = YamlConfiguration.loadConfiguration(file)
        val section = root.getConfigurationSection("vouchers") ?: return

        for (id in section.getKeys(false)) {
            val s = section.getConfigurationSection(id) ?: continue
            val actionTag = s.getString("custom-action")?.uppercase()

            val spec = VoucherSpec(
                id            = id,
                displayName   = s.getString("display-name")!!,
                material      = Material.valueOf(s.getString("material")!!.uppercase()),
                lore          = s.getStringList("lore"),
                rightClickable= s.getBoolean("right-clickable", true),
                rewardDef     = parseReward(s.getConfigurationSection("reward")),
                customAction  = buildCustomAction(actionTag),
                rarity        = Rarity.valueOf(s.getString("rarity")!!.uppercase()),
                tier          = Tier.valueOf(s.getString("tier")!!.uppercase())
            )
            registry[id] = spec
        }
        Bukkit.getLogger().info("[RunicOverlord] Loaded ${registry.size} vouchers.")
    }

    private fun parseReward(sec: org.bukkit.configuration.ConfigurationSection?): RewardDef? {
        val id = sec?.name                          // node name, e.g. "rare_fishing_boost"
        val rewardSec = sec?.getConfigurationSection("reward")
            ?: return null

        val type = rewardSec.getString("type")?.uppercase()
        if (type == null) {
            Bukkit.getLogger().severe("Voucher '$id' is missing reward.type")
            return null
        }

        if (sec == null) return null

        return when (sec.getString("type")!!.uppercase()) {
            "GIVE_ITEM"      -> RewardDef.GiveItem(
                item   = Material.valueOf(sec.getString("item")!!.uppercase()),
                amount = sec.getInt("amount", 1)
            )
            "GIVE_XP"        -> RewardDef.GiveXP(sec.getInt("amount", 0))
            "TELEPORT_SPAWN" -> RewardDef.TeleportSpawn

            else -> null
        }
    }
    private fun buildCustomAction(tag: String?): VoucherAction? =
        VoucherActions.byTag(tag)

    /* ───────── create ItemStack ───────── */
    fun createVoucher(id: String): ItemStack? {
        val spec = registry[id] ?: return null
        val item = ItemStack(spec.material)
        val meta = item.itemMeta ?: return item
        meta.setDisplayName(spec.displayName)
        meta.lore = spec.lore
        meta.addEnchant(Enchantment.INFINITY, 1, true)
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)

        item.itemMeta = meta
        return item
    }

    /* ───────── lookup helpers ───────── */
    fun matchVoucher(item: ItemStack): VoucherSpec? {
        val meta = item.itemMeta ?: return null
        return registry.values.firstOrNull {
            meta.displayName == it.displayName && meta.lore == it.lore
        }
    }

    fun allSpecs(): Collection<VoucherSpec> = registry.values
}
