package com.trevorfarias.runic_overlord.voucher

import com.trevorfarias.runic_overlord.gear.GearFactory
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack

/**
 * Drag-and-drop a “Quality Reroll Voucher” onto any Runic Overlord gear to
 * roll a brand-new 1-100 quality and rebuild all stats/lore.
 *
 * YAML id expected:  quality_reroll
 * custom-action tag: REROLL_QUALITY_EPIC   (ignored here – we key off the id)
 */
class QualityRerollVoucherListener : Listener {

    @EventHandler
    fun onVoucherApply(e: InventoryClickEvent) {
        val p        = e.whoClicked as? Player ?: return
        val voucher  = e.cursor ?: return
        val spec     = VoucherFactory.matchVoucher(voucher) ?: return
        if (spec.id != "quality_reroll") return                // <-- your YAML id

        val gear = e.currentItem ?: return
        if (gear.type.isAir || GearFactory.getSpec(gear) == null) {
            p.sendMessage("§cThat item isn’t Runic Overlord gear.")
            return
        }

        // ---- strip existing attribute modifiers (avoids duplicates) ----
        gear.itemMeta?.let { meta ->
            meta.attributeModifiers?.keys()?.forEach { meta.removeAttributeModifier(it) }
            gear.itemMeta = meta
        }

        // ---- reroll & rebuild stats/lore ------------------------------
        val newQ = GearFactory.rollQuality()
        GearFactory.markIdentified(gear, newQ)                 // rewrites lore & stats :contentReference[oaicite:0]{index=0}

        consumeOne(voucher, p)
        e.isCancelled = true
        p.sendMessage("§5[Quality]§a Gear quality rerolled to §d$newQ%§a!")
        p.updateInventory()
    }

    private fun consumeOne(stack: ItemStack, p: Player) {
        stack.amount -= 1
        if (stack.amount > 0) p.inventory.addItem(stack)       // return leftovers
    }
}
