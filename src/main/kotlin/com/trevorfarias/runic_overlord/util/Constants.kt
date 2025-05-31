package com.trevorfarias.runic_overlord.util

import com.trevorfarias.runic_overlord.RunicOverlord
import org.bukkit.NamespacedKey

object Constants {
    val RUNE_SLOT_KEY = NamespacedKey(RunicOverlord.instance, "rune_slots")
    val RUNE_IDS_KEY = NamespacedKey(RunicOverlord.instance, "rune_ids")
    val IS_RUNE_REMOVER = NamespacedKey(RunicOverlord.instance, "is_rune_remover")

    const val MAX_RUNES = 3
}
