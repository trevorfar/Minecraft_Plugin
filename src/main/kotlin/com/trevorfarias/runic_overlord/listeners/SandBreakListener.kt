package com.trevorfarias.runic_overlord.listeners


import com.trevorfarias.runic_overlord.RunicOverlord
import com.trevorfarias.runic_overlord.voucher.VoucherFactory
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import kotlin.random.Random

class SandBreakListener : Listener {

    @EventHandler
    fun onSandBreak(event: BlockBreakEvent) {
        if (event.block.type != Material.BEDROCK) return
        if (Random.nextDouble() >= 0.5) return

        val player = event.player

        player.inventory.addItem(VoucherFactory.createVoucher("diamond")!!)
        player.sendMessage("📜 You found a ${RunicOverlord.VOUCHER_NAME}!")
    }
}