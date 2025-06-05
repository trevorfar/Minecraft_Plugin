package com.trevorfarias.runic_overlord.voucher

import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.event.block.Action

class VoucherUseListener : Listener {

    @EventHandler
    fun onUseVoucher(event: PlayerInteractEvent) {
        val player = event.player
        val item = event.item ?: return
        val spec = VoucherFactory.matchVoucher(item) ?: return

        val action = event.action
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) return

        if (action.name.contains("RIGHT") && !spec.rightClickable) return

        event.isCancelled = true
        consumeOneItem(player, item)
        spec.reward?.let { it(player) }
        player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f)
    }

    private fun consumeOneItem(player: org.bukkit.entity.Player, item: ItemStack) {
        if (item.amount <= 1) {
            player.inventory.removeItem(item)
        } else {
            item.amount -= 1
        }
    }
}
