package com.trevorfarias.runic_overlord.util

import com.trevorfarias.runic_overlord.RunicOverlord
import org.bukkit.NamespacedKey

object Constants {
    val RUNE_SLOT_KEY = NamespacedKey(RunicOverlord.instance, "rune_slots")
    val RUNE_IDS_KEY = NamespacedKey(RunicOverlord.instance, "rune_ids")
    val IS_RUNE_REMOVER = NamespacedKey(RunicOverlord.instance, "is_rune_remover")
    val GEAR_ID_KEY       = NamespacedKey(RunicOverlord.instance, "gear_id")
    val GEAR_QUALITY_KEY  = NamespacedKey(RunicOverlord.instance, "gear_quality")
    val GEAR_IDENTIFIED_KEY = NamespacedKey(RunicOverlord.instance, "gear_identified")
    val IDENTIFIER_KEY = NamespacedKey(RunicOverlord.instance, "is_identifier")
    const val MAX_RUNES = 3
    const val NEPTUNE_CHANCE = 0.5 //0.05
    const val WILLOWS_CHANCE = 0.1 // 0.1

}


