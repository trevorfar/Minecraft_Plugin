package com.trevorfarias.runic_overlord.gear

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

class GearGuiCommand(private val plugin: JavaPlugin) : CommandExecutor, Listener {

    // Called when player runs /rofgear
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cOnly players can use this command!")
            return true
        }
        openGearGui(sender)
        return true
    }

    // Opens the gear showcase GUI
    private fun openGearGui(player: Player) {
        val allSpecs = GearFactory.getAllSpecs() // Implement this to return a list of all GearSpecs
        .sortedWith(
            compareBy<GearSpec> { it.rarity.weight }          // 1 = LEGENDARY … 60 = COMMON
                .thenByDescending { it.tier }                 // III before II before I
                .thenBy { it.displayName.lowercase() }
        )
        val guiSize = ((allSpecs.size + 8) / 9) * 9 // Round up to nearest multiple of 9 (Bukkit GUIs must be multiples of 9)
        val gui = Bukkit.createInventory(null, guiSize, "§3All Custom Gear")

        for ((i, spec) in allSpecs.withIndex()) {
            // Build item for GUI with extra location lore
            val item = buildGuiItem(spec, 100) // use 100% quality for showcase
            gui.setItem(i, item)
        }

        player.openInventory(gui)

        Bukkit.getPluginManager().registerEvents(object : Listener {
            @org.bukkit.event.EventHandler
            fun onInvClick(e: InventoryClickEvent) {
                if (e.whoClicked == player && e.view.title == "§3All Custom Gear") {
                    e.isCancelled = true
                }
            }
        }, plugin)
    }

    // Build the gear item with GUI-only lore line
    private fun buildGuiItem(spec: GearSpec, quality: Int): ItemStack {
        val item = GearFactory.create(spec.id, quality) ?: return ItemStack(org.bukkit.Material.BARRIER)
        val meta = item.itemMeta!!
        val guiLore = meta.lore?.toMutableList() ?: mutableListOf()
        val resolvedNames = spec.locationTags.mapNotNull { GearFactory.locationTagPrettyNames[it] }
        if (resolvedNames.isNotEmpty()) {
            guiLore += "§fWhere to Find:"
            resolvedNames.forEach { prettyName ->
                guiLore += "§9[$prettyName]"
            }
        }
        meta.lore = guiLore
        item.itemMeta = meta
        return item
    }
}
