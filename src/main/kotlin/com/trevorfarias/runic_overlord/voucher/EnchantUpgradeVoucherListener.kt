package com.trevorfarias.runic_overlord.voucher

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.EnchantmentStorageMeta
import java.util.UUID
import kotlin.random.Random

class EnchantUpgradeVoucherListener : Listener {

    /** Player UUID → voucher ItemStack (real reference) */
    private val pendingVoucher = mutableMapOf<UUID, ItemStack>()

    /** Player UUID → slot index of gear being upgraded */
    private val pendingGearSlot = mutableMapOf<UUID, Int>()

    /* ───────── Drag voucher onto gear ───────── */
    @EventHandler
    fun onVoucherApply(e: InventoryClickEvent) {
        val p       = e.whoClicked as? Player ?: return
        val cursor  = e.cursor ?: return
        val spec    = VoucherFactory.matchVoucher(cursor) ?: return
        val gear    = e.currentItem ?: return
        if (gear.type.isAir) return
        if (gear.type.maxDurability <= 0) return


        when (spec.id) {
            "epic_enchant_upgrade"   -> handleEpic(p, gear, cursor, e)
            "mythic_enchant_upgrade" -> handleMythic(p, gear, cursor, e)
        }
    }

    /* ── EPIC: random +1 (max +1 over vanilla) ── */
    fun handleEpic(p: Player, gear: ItemStack, voucher: ItemStack, e: InventoryClickEvent) {
        val upgradable = gear.itemMeta?.enchants?.filter { (ench, lvl) ->
            lvl < ench.maxLevel + 1
        } ?: emptyMap()

        if (upgradable.isEmpty()) {
            p.sendMessage("§cNothing on that item can be upgraded (max +1).")
            return
        }

        val (ench, lvl) = upgradable.entries.random(Random)
        upgradeEnchant(gear, ench, lvl + 1, ench.maxLevel + 1)

        consumeOne(voucher, p)
        e.isCancelled = true
        p.sendMessage("§5[Epic]§a Upgraded §d${ench.key.key}§a to §d${lvl + 1}§a!")
    }

    /* ── MYTHIC: choose via GUI (max +2) ── */
    fun handleMythic(p: Player, gear: ItemStack, voucher: ItemStack, e: InventoryClickEvent) {
        val upgradable = gear.itemMeta?.enchants?.filter { (ench, lvl) ->
            lvl < ench.maxLevel + 2
        } ?: emptyMap()

        if (upgradable.isEmpty()) {
            p.sendMessage("§cNothing on that item can be upgraded (max +2).")
            return
        }

        pendingVoucher[p.uniqueId]  = voucher        // store real stack
        pendingGearSlot[p.uniqueId] = e.slot

        e.setCursor(ItemStack(Material.AIR))
        e.isCancelled = true

        openChoiceGUI(p, upgradable)
    }

    /* ───────── Build GUI ───────── */
    private fun openChoiceGUI(p: Player, enchants: Map<Enchantment, Int>) {
        val size = ((enchants.size + 8) / 9) * 9
        val gui  = Bukkit.createInventory(null, size, "§6Choose Upgrade")

        enchants.keys.forEachIndexed { i, ench ->
            val book = ItemStack(Material.ENCHANTED_BOOK)
            val meta = book.itemMeta as EnchantmentStorageMeta
            meta.addStoredEnchant(ench, 1, true)
            meta.setDisplayName("§e${ench.key.key.replace('_', ' ').capitalize()} §7→ +1")
            meta.lore = listOf("§7Click to upgrade")
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
            book.itemMeta = meta
            gui.setItem(i, book)
        }
        p.openInventory(gui)
    }

    /* ───────── Click inside GUI ───────── */
    @EventHandler
    fun onChoice(e: InventoryClickEvent) {
        if (e.view.title != "§6Choose Upgrade") return
        e.isCancelled = true

        val p = e.whoClicked as? Player ?: return
        val voucher = pendingVoucher[p.uniqueId] ?: return
        val gearSlot = pendingGearSlot[p.uniqueId] ?: return

        val clicked = e.currentItem ?: return
        val ench = (clicked.itemMeta as? EnchantmentStorageMeta)
            ?.storedEnchants?.keys?.firstOrNull() ?: return

        val gear = p.inventory.getItem(gearSlot) ?: return
        val current = gear.itemMeta!!.getEnchantLevel(ench)
        upgradeEnchant(gear, ench, current + 1, ench.maxLevel + 2)

        consumeOne(voucher, p)        // really removes 1, returns leftovers if any
        clearPending(p)

        p.updateInventory()
        p.closeInventory()
        p.sendMessage("§6[Mythic]§a Upgraded §e${ench.key.key}§a to §e${current + 1}§a!")
    }

    /* ───────── Refund if player quits or closes without choosing ───────── */
    @EventHandler
    fun onClose(e: InventoryCloseEvent) {
        if (e.view.title != "§6Choose Upgrade") return
        refundVoucher(e.player as Player)
    }

    @EventHandler
    fun onQuit(e: PlayerQuitEvent) {
        refundVoucher(e.player)
    }

    private fun refundVoucher(p: Player) {
        val voucher = pendingVoucher.remove(p.uniqueId) ?: return
        p.inventory.addItem(voucher)          // give full stack back
        pendingGearSlot.remove(p.uniqueId)
    }

    private fun clearPending(p: Player) {
        pendingVoucher.remove(p.uniqueId)
        pendingGearSlot.remove(p.uniqueId)
    }

    /* ───────── Helpers ───────── */
    private fun upgradeEnchant(item: ItemStack, ench: Enchantment, newLvl: Int, cap: Int) {
        val meta = item.itemMeta!!
        meta.addEnchant(ench, newLvl.coerceAtMost(cap), true)
        item.itemMeta = meta
    }

    private fun consumeOne(stack: ItemStack, p: Player) {
        stack.amount -= 1
        if (stack.amount > 0) {
            p.inventory.addItem(stack)        // return leftovers
        }
    }
}
