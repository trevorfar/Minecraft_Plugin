package com.trevorfarias.runic_overlord.voucher

import com.trevorfarias.runic_overlord.RunicOverlord
import com.trevorfarias.runic_overlord.runes.RuneApplyListener
import com.trevorfarias.runic_overlord.runes.RuneFactory
import com.trevorfarias.runic_overlord.util.Constants
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

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

        RuneApplyListener.PendingRuneRemovals.map[player] = armor
        Bukkit.getScheduler().runTask(RunicOverlord.instance, Runnable {
            player.setItemOnCursor(ItemStack(Material.AIR))
            player.openInventory(gui)
        })
    }
}

data class VoucherSpec(
    val id: String,
    val displayName: String,
    val rightClickable: Boolean = true,
    val lore: List<String>,
    val material: Material,
    val reward: ((player: Player) -> Unit)? = null,
    val customAction: ((player: Player, target: ItemStack, cursor: ItemStack, event: InventoryClickEvent) -> Boolean)? = null
)

object VoucherFactory {

    // Map of all defined vouchers by ID
    private val voucherRegistry: Map<String, VoucherSpec> = mapOf(
        "epic_enchant_upgrade" to VoucherSpec(
            id = "epic_enchant_upgrade",
            displayName = "§5Epic Enchant Upgrade Voucher",
            material = Material.ENCHANTED_BOOK,
            rightClickable = false,
            lore = listOf(
                "§7Drag & drop onto gear to §drandomly §7upgrade",
                "§7one enchantment by §d+1§7.",
                "§8Cannot exceed vanilla max by more than §d+1§8."
            )
        ),
        "mythic_enchant_upgrade" to VoucherSpec(
            id = "mythic_enchant_upgrade",
            displayName = "§6Mythic Enchant Upgrade Voucher",
            material = Material.ENCHANTED_BOOK,
            rightClickable = false,
            lore = listOf(
                "§7Drag & drop onto gear to §echoose",
                "§7an enchantment and add §e+1§7.",
                "§8May exceed vanilla max by §e+2§8."
            )
        ),
        "diamond" to VoucherSpec(
            id = "diamond",
            displayName = "§bDiamond Voucher",
            lore = listOf("§7Right-click to redeem", "§7for 1 Diamond 💎"),
            material = Material.FIREWORK_STAR,
            reward = { player ->
                player.inventory.addItem(ItemStack(Material.DIAMOND))
                player.sendMessage("💎 You got a Diamond!")
            }
        ),
        "xp" to VoucherSpec(
            id = "xp",
            displayName = "§aXP Voucher",
            lore = listOf("§7Right-click to redeem", "§7for 500 XP"),
            material = Material.FIREWORK_STAR,
            reward = { player ->
                player.giveExp(500)
                player.sendMessage("📘 You gained 500 XP!")
            }
        ),
        "teleport" to VoucherSpec(
            id = "teleport",
            displayName = "§dTeleport Voucher",
            lore = listOf("§7Right-click to return to spawn"),
            material = Material.FIREWORK_STAR,
            reward = { player ->
                player.teleport(player.world.spawnLocation)
                player.sendMessage("🌀 Teleported to spawn!")
            }
        ),
        "rune_remover" to VoucherSpec(
            id = "rune_remover",
            displayName = "§cRune Remover",
            lore = listOf("§7Drag onto an item to choose a rune to remove"),
            material = Material.BRUSH,
            rightClickable = false,
            customAction = { player, armorItem, cursorItem, event ->
                val cursorMeta = cursorItem.itemMeta ?: return@VoucherSpec false
                if (!cursorMeta.persistentDataContainer.has(Constants.IS_RUNE_REMOVER, PersistentDataType.BYTE)) return@VoucherSpec false

                val armorMeta = armorItem.itemMeta ?: return@VoucherSpec false
                val pdc = armorMeta.persistentDataContainer
                val runeIds = pdc.get(Constants.RUNE_IDS_KEY, PersistentDataType.STRING)
                    ?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

                if (runeIds.isEmpty()) {
                    player.sendMessage("§eThis armor has no runes.")
                    return@VoucherSpec true
                }

                RuneApplyListener.PendingVoucherUse.map[player] = cursorItem.clone()
                RuneApplyListener.PendingRuneRemovals.map[player] = armorItem.clone()
                RuneGUI.openRuneRemovalGUI(player, armorItem.clone(), runeIds)
                event.isCancelled = true
                return@VoucherSpec true
            }

        )

        //FIXES:
        //Refund remover, stop removing other item in inventory

    )

    // Returns an ItemStack representing the given voucher
    fun createVoucher(id: String): ItemStack? {
        val spec = voucherRegistry[id] ?: return null

        val item = ItemStack(spec.material)
        val meta = item.itemMeta ?: return item

        meta.setDisplayName(spec.displayName)
        meta.lore = spec.lore
        meta.addEnchant(Enchantment.INFINITY, 1000, true)
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)

        if (id == "rune_remover") {
            val key = NamespacedKey(RunicOverlord.instance, "is_rune_remover")
            meta.persistentDataContainer.set(key, PersistentDataType.BYTE, 1)
        }

        item.itemMeta = meta
        return item
    }



    // Matches an ItemStack to a VoucherSpec
    fun matchVoucher(item: ItemStack): VoucherSpec? {
        val meta = item.itemMeta ?: return null
        return voucherRegistry.values.firstOrNull {
            meta.displayName == it.displayName && meta.lore == it.lore
        }
    }
}
