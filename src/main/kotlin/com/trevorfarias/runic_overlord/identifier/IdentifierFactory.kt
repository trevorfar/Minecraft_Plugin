/* src/main/kotlin/com/trevorfarias/runic_overlord/identifier/IdentifierFactory.kt */
package com.trevorfarias.runic_overlord.identifier

import com.trevorfarias.runic_overlord.util.Constants
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType

object IdentifierFactory {
    fun create(): ItemStack =
        ItemStack(Material.PAPER).apply {
            val meta = itemMeta!!
            meta.setDisplayName("§eItem Identifier")
            meta.lore = listOf("§7Drag onto an", "§7§oUnidentified Item")
            // mark it in PDC so the listener can recognise it
            meta.persistentDataContainer.set(Constants.IDENTIFIER_KEY, PersistentDataType.INTEGER, 1)
            itemMeta = meta
        }
    fun isIdentifier(item: ItemStack?): Boolean =
        item?.itemMeta?.persistentDataContainer?.has(Constants.IDENTIFIER_KEY, PersistentDataType.INTEGER) == true


}
