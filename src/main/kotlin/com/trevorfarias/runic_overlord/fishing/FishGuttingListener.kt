package com.trevorfarias.runic_overlord.fishing

import com.trevorfarias.runic_overlord.fishing.FishingConfig.tiers
import com.trevorfarias.runic_overlord.fishing.FishingKeys
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.*
import kotlin.random.Random

class FishGuttingListener : Listener {

    /* ───────── Player sneak-right-clicks with a fish in main-hand ───────── */
    @EventHandler
    fun onGutting(e: PlayerInteractEvent) {
        if (!e.hasItem()) return
        if (e.hand != EquipmentSlot.HAND) return              // main hand only
        if (!e.action.isRightClick) return
        val p = e.player
        if (!p.isSneaking) return                             // must sneak

        val fish = e.item ?: return
        val meta = fish.itemMeta ?: return

        val tierId = meta.persistentDataContainer
            .get(FishingKeys.FISH_TIER, PersistentDataType.STRING) ?: return
        val tier  = tiers[tierId] ?: return

        when (tier.id) {
            "epic" -> {
                val qty = if (Random.nextDouble() < 0.25) 2 else 1
                giveRemains(p, "epic_remains", "§5Epic Fish Remains", qty)
            }
            "mythical" -> {
                giveRemains(p, "mythic_remains", "§6Mythic Fish Remains", 1)
            }
            else -> {
                p.sendMessage("§eOnly Epic or Mythic fish yield special remains.")
                return
            }
        }

        /* consume the fish */
        if (fish.amount <= 1)
            p.inventory.setItemInMainHand(null)
        else
            fish.amount -= 1
        e.isCancelled = true
    }

    /* ───────── build & give remains ───────── */
    private fun giveRemains(player: Player, id: String, name: String, amount: Int) {
        val item = ItemStack(Material.RABBIT_HIDE, amount)
        val meta = item.itemMeta!!
        meta.setDisplayName(name)
        meta.lore = listOf("§7Combine two to craft a voucher.")
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
        meta.persistentDataContainer.apply {
            set(NamespacedKey(player.server.pluginManager.getPlugin("RunicOverlord")!!, "remain_id"),
                PersistentDataType.STRING, id)
            set(NamespacedKey(player.server.pluginManager.getPlugin("RunicOverlord")!!, "uuid"),
                PersistentDataType.STRING, UUID.randomUUID().toString())
        }
        item.itemMeta = meta
        player.inventory.addItem(item)
        player.sendMessage("§aYou gutted the fish and received $amount ${name.removePrefix("§5").removePrefix("§6")}§a!")
    }
}
