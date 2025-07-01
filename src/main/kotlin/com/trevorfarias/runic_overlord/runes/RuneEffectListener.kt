package com.trevorfarias.runic_overlord.runes

import com.trevorfarias.runic_overlord.RunicOverlord
import com.trevorfarias.runic_overlord.util.Constants
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.inventory.EquipmentSlot

class RuneEffectListener : Listener {

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        applyAllRunes(event.player)
    }

    @EventHandler
    fun onRespawn(event: PlayerRespawnEvent) {
        val plugin = event.player.server.pluginManager.getPlugin("RunicOverlord")
            ?: return // Safely abort if plugin isn't loaded

        event.player.server.scheduler.runTaskLater(
            plugin,
            Runnable {
                applyAllRunes(event.player)
            },
            2L
        )
    }


    @EventHandler
    fun onArmorInteract(event: InventoryClickEvent) {
        val player = event.whoClicked as? org.bukkit.entity.Player ?: return
        if (event.slotType == InventoryType.SlotType.ARMOR) {
            val plugin = RunicOverlord.instance
            player.server.scheduler.runTaskLater(
                plugin,
                Runnable {
                    applyAllRunes(player)
                },
                2L
            )
        }
    }

    private fun applyAllRunes(player: org.bukkit.entity.Player) {
        val equipment = player.equipment ?: return

        listOf(
            EquipmentSlot.HEAD to equipment.helmet,
            EquipmentSlot.CHEST to equipment.chestplate,
            EquipmentSlot.LEGS to equipment.leggings,
            EquipmentSlot.FEET to equipment.boots
        ).forEach { (slot, item) ->
            if (item == null || item.type.isAir) return@forEach

            val pdc = item.itemMeta?.persistentDataContainer ?: return@forEach
            val runeIdsRaw = pdc.get(Constants.RUNE_IDS_KEY, org.bukkit.persistence.PersistentDataType.STRING)
            val runeIds = runeIdsRaw?.split(",")?.filter { it.isNotBlank() } ?: return@forEach

            if (runeIds.isEmpty()) return@forEach

            var modified = item.clone()
            for (runeId in runeIds) {
                val rune = RuneFactory.getRuneSpecById(runeId) ?: continue
                modified = rune.applyModifier(modified)
            }

            equipment.setItem(slot, modified)
        }
    }

}
