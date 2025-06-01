package com.trevorfarias.runic_overlord.listeners

import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector

class BarrierWand : Listener {

    private val wandName = "§bBarrier Wand"

    @EventHandler
    fun onRightClick(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val player = event.player
        val item = player.inventory.itemInMainHand

        if (!isBarrierWand(item)) return

        val clicked = event.clickedBlock ?: return
        val face = event.blockFace
        val world = clicked.world

        // One block outside the clicked face
        val startX = clicked.x + face.modX
        val startY = clicked.y + face.modY + 1
        val startZ = clicked.z + face.modZ

        for (y in startY..world.maxHeight) {
            val block = world.getBlockAt(startX, y, startZ)
            if (block.type == Material.AIR) {
                block.type = Material.BARRIER
            }
        }

        player.sendMessage("§aBarrier column placed at face position.")
        event.isCancelled = true
    }

    private fun isBarrierWand(item: ItemStack?): Boolean {
        if (item == null || item.type != Material.STICK) return false
        val meta = item.itemMeta ?: return false
        return meta.displayName == wandName
    }

    companion object {
        fun createWand(): ItemStack {
            return ItemStack(Material.STICK).apply {
                itemMeta = itemMeta?.apply {
                    setDisplayName("§bBarrier Wand")
                    addEnchant(Enchantment.INFINITY, 1, true)
                    addItemFlags(ItemFlag.HIDE_ENCHANTS)
                }
            }
        }
    }
}
