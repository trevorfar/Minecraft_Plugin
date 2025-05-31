package com.trevorfarias.runic_overlord.voucher

import com.trevorfarias.runic_overlord.RunicOverlord
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

data class VoucherSpec(
    val id: String,
    val displayName: String,
    val lore: List<String>,
    val material: Material,
    val reward: (player: org.bukkit.entity.Player) -> Unit
)

object VoucherFactory {

    // Map of all defined vouchers by ID
    private val voucherRegistry: Map<String, VoucherSpec> = mapOf(
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
            reward = { player ->
                player.sendMessage("Rune removed!")
            }
        )

    )

    // Returns an ItemStack representing the given voucher
    fun createVoucher(id: String): ItemStack? {
        val spec = voucherRegistry[id] ?: return null

        val item = ItemStack(spec.material)
        val meta = item.itemMeta ?: return item

        meta.setDisplayName(spec.displayName)
        meta.lore = spec.lore
        meta.addEnchant(Enchantment.SHARPNESS, 1, true)
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
