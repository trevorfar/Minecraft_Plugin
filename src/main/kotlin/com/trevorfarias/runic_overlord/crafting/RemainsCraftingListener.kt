package com.trevorfarias.runic_overlord.crafting

import com.trevorfarias.runic_overlord.voucher.VoucherFactory
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.inventory.CraftingInventory
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

class RemainsCraftingListener : Listener {

    private val plugin = Bukkit.getPluginManager().getPlugin("RunicOverlord")!!
    private val remainKey = NamespacedKey(plugin, "remain_id")

    @EventHandler
    fun onPrepareCraft(e: PrepareItemCraftEvent) {
        val inv  = e.inventory as CraftingInventory
        val grid = inv.matrix.filterNotNull().filter { !it.type.isAir }

        // need exactly 2 items
        if (grid.size != 2) { inv.result = null; return }

        val firstId  = grid[0].itemMeta?.persistentDataContainer
            ?.get(remainKey, PersistentDataType.STRING)
        val secondId = grid[1].itemMeta?.persistentDataContainer
            ?.get(remainKey, PersistentDataType.STRING)

        // both items must be remains and of the SAME type
        if (firstId == null || firstId != secondId) { inv.result = null; return }

        val resultVoucherId = when (firstId) {
            "epic_remains"   -> "epic_enchant_upgrade"
            "mythic_remains" -> "mythic_enchant_upgrade"
            else             -> null
        } ?: run { inv.result = null; return }

        inv.result = VoucherFactory.createVoucher(resultVoucherId)
    }
}
