package com.trevorfarias.runic_overlord.gear

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.plugin.java.JavaPlugin

class GearTestCommand(private val plugin: JavaPlugin) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val player = sender as? Player ?: return false
        if (args.isEmpty()) return player.fail("Usage: /rofgear test <gear_id>")

        val gearId = args[0].lowercase()
        sender.sendMessage("§7Available: §f" + GearFactory.allIds().joinToString(", "))

        val spec = GearFactory.getSpecById(gearId) ?: return player.fail("§cNo such gear: $gearId")

        // Create a fake gear item (100% quality)
        val testItem = GearFactory.create(gearId, 100) ?: return player.fail("§cFailed to create test item.")
        sender.sendMessage("§7Available: §f" + GearFactory.allIds().joinToString(", "))

        // Simulate damage eventsender.sendMessage("§7Available: §f" + GearFactory.allIds().joinToString(", "))
        val dummyDamager = player // self-hit for testing purposes
        val event = EntityDamageByEntityEvent(dummyDamager, player, EntityDamageEvent.DamageCause.ENTITY_ATTACK, 1.0)

        // Force effect logic to run manually
        CustomGearListener.simulateEffect(spec.id, testItem, player, event)

        player.sendMessage("§aSimulated §f$gearId §aeffect on you.")
        return true
    }

    private fun Player.fail(msg: String): Boolean {
        sendMessage(msg)
        return true
    }
}
