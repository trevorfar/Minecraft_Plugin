//package com.trevorfarias.runic_overlord.integration.mythic
//
//import com.trevorfarias.runic_overlord.gear.GearFactory
//import io.lumine.mythic.api.drops.IDrop              // Mythic 5.x package :contentReference[oaicite:3]{index=3}
//import io.lumine.mythic.api.drops.DropMetadata
//import org.bukkit.inventory.ItemStack
//
///**
// * Native MythicMobs drop that yields Runic-Overlord gear.
// *
// * QUALITY argument:
// *   • UNIDENTIFIED   – hidden % (identifier needed)
// *   • RANDOM         – roll 0-100 with GearFactory
// *   • 0-100 integer  – force a specific quality
// */
//class RunicGearDrop(
//    private val gearId: String,
//    private val qualityArg: String = "RANDOM"
//) : IDrop {
//
//    override fun getDrop(meta: DropMetadata?): MutableCollection<ItemStack> {
//        val stack = when (qualityArg.uppercase()) {
//            "UNIDENTIFIED" -> GearFactory.createUnidentified(gearId)
//            "RANDOM"       -> GearFactory.create(gearId)                  // random %
//            else           -> qualityArg.toIntOrNull()?.let { q ->
//                GearFactory.create(gearId, q.coerceIn(0, 100))
//            }
//        }
//        return stack?.let { mutableListOf(it) } ?: mutableListOf()
//    }
//}
