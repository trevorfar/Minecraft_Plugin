// src/main/kotlin/com/trevorfarias/runic_overlord/voucher/VoucherGuiCommand.kt
package com.trevorfarias.runic_overlord.voucher

import com.trevorfarias.runic_overlord.RunicOverlord
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import kotlin.math.ceil

class VoucherGuiCommand(private val plugin: JavaPlugin) : CommandExecutor, Listener {

    private val TITLE = "§dAll Vouchers"       // colourful header

    /** /rofvoucher  → GUI   /rofvoucher reload  → re-read YAML */
    override fun onCommand(
        sender: CommandSender,
        cmd:    Command,
        label:  String,
        args:   Array<out String>
    ): Boolean {
        // --- open GUI ------------------------------------------------------
        if (sender !is Player) {
            sender.sendMessage("§cOnly players can open the voucher GUI.")
            return true
        }
        openGui(sender)
        return true
    }

    /* ─────────────────────────────────────────────────────────────────── */

    private fun openGui(player: Player) {
        val specs   = VoucherFactory.allSpecs()
        val rows    = ((specs.size + 8) / 9).coerceAtMost(6)        // max 54 slots
        val gui     = Bukkit.createInventory(null, rows * 9, "§dAll Vouchers")

        specs.forEachIndexed { i, spec ->
            val item = VoucherFactory.createVoucher(spec.id) ?: return@forEachIndexed
            gui.setItem(i, item)
        }

        player.openInventory(gui)

        // ─────────────────── block item pickup / move ────────────────────
        Bukkit.getPluginManager().registerEvents(object : Listener {
            @EventHandler
            fun onInvClick(e: InventoryClickEvent) {
                if (e.whoClicked == player && e.view.title == "§dAll Vouchers") {
                    e.isCancelled = true          // no grabbing, no shift-clicks
                }
            }
        }, plugin)
    }

    /** optional: show rarity / tier in the lore so players can sort mentally */
    private fun addExtraLore(item: ItemStack, spec: VoucherSpec): ItemStack {
        val meta  = item.itemMeta!!
        val lore  = meta.lore?.toMutableList() ?: mutableListOf()
        lore += "§7Rarity: §r${spec.rarity}"      // simple, no colours yet
        lore += "§7Tier:   §r${spec.tier}"
        meta.lore = lore
        item.itemMeta = meta
        return item
    }

    /* ── cancel clicks so nobody steals the showcase items ─────────────── */
    @EventHandler
    fun onInvClick(e: InventoryClickEvent) {
        if (e.view.title != TITLE) return
        e.isCancelled = true
    }
}
