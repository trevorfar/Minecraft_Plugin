package com.trevorfarias.runic_overlord.voucher

import com.trevorfarias.runic_overlord.gear.GearFactory
import com.trevorfarias.runic_overlord.runes.RuneRemoverGuiListener
import com.trevorfarias.runic_overlord.util.Constants
import org.bukkit.*
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack

import org.bukkit.potion.PotionEffect
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.potion.PotionEffectType
import org.bukkit.persistence.PersistentDataType

object VoucherActions {

    private val registry = mutableMapOf<String, VoucherAction>()

    /* helper so you can wire actions in a one-liner */
    fun register(tag: String, action: VoucherAction) {
        registry[tag.uppercase()] = action
    }
    fun byTag(tag: String?) = registry[tag?.uppercase()]

    // ── Existing actions ──────────────────────────────────────────────
    init {
        register("APPLY_UNBREAKING_I", applyUnbreakingI())
        register("ADD_SOCKET",         addSocket())
        register("REPAIR_25",          repair25())
        register("STAT_REALLOC",       statRealloc())
        register("APPLY_HASTE_I",      applyHasteI())
        register("APPLY_LUCK_II_30M",  applyLuckII30m())
        register("REROLL_QUALITY_EPIC", rerollQualityEpic())
        register("RUNE_REMOVER", openRuneRemovalGuiAction())


    }
    fun openRuneRemovalGuiAction(): VoucherAction {
        return fun(player, gear, voucher, e): Boolean {
            if (e == null) return false
            return RuneRemoverGuiListener.handleRuneDrag(player, gear, voucher, e)
        }
    }
    /* -------------------------------------------------------------------- */
    /** Adds Unbreaking I to any tool / armour piece. */
    fun applyUnbreakingI(): VoucherAction = { player, gear, voucher, e ->
        if (gear.type.isAir || gear.type.maxDurability <= 0) {
            player.sendMessage("§cDrag this onto a valid tool or armour item.")
        }
        val meta = gear.itemMeta!!
        if (meta.hasEnchant(Enchantment.UNBREAKING)) {
            player.sendMessage("§cThat item already has Unbreaking.")
        }
        meta.addEnchant(Enchantment.UNBREAKING, 3, true)
        gear.itemMeta = meta
        consumeOne(voucher, player)
        if (e != null) {
            e.isCancelled = true
        }
        player.playSound(player.location, Sound.ITEM_ARMOR_EQUIP_DIAMOND, 1f, 1.1f)
        true
    }

    fun rerollQualityEpic():VoucherAction = { p, gear, voucher, e ->
        if (!gear.type.isAir || GearFactory.getSpec(gear) != null) {

            gear.itemMeta?.let { m ->
                m.attributeModifiers?.keys()?.forEach { m.removeAttributeModifier(it) }; gear.itemMeta = m
            }
            val q = GearFactory.rollQuality()
            GearFactory.markIdentified(gear, q)
            consumeOne(voucher, p); if (e != null) {
                e.isCancelled = true
            }
            p.sendMessage("§5[Quality]§a rerolled to §d$q%§a!"); true
        }else {
            p.sendMessage("§cThat item isn’t Runic Overlord gear."); false
        }
    }

    /* -------------------------------------------------------------------- */
    /** Adds one rune socket by incrementing an integer PDC key. */
    fun addSocket():VoucherAction = { player, gear, voucher, e ->
        val meta = gear.itemMeta as ItemMeta
        val key = Constants.GEAR_SOCKET_COUNT_KEY
        val container = meta.persistentDataContainer
        val pdc = meta.persistentDataContainer
        val current = container.get(key, PersistentDataType.INTEGER) ?: 0
        meta.persistentDataContainer.set(key, PersistentDataType.INTEGER, current + 1)
        gear.itemMeta = meta
        consumeOne(voucher, player)
        if (e != null) {
            e.isCancelled = true
        }
        player.sendMessage("§9+1 rune socket added!")
        true
    }

    /* -------------------------------------------------------------------- */
    /** Repairs 25 % of the item’s durability (drag‑and‑drop). */
    fun repair25(): VoucherAction = { player, gear, voucher, e ->
        if (gear.type.isAir || gear.type.maxDurability <= 0) {
            player.sendMessage("§cDrag onto a damaged item.") }
        if (gear.durability == 0.toShort()) {                      // already perfect
            player.sendMessage("§aThat item is already fully repaired.")
        }
        val maxDur = gear.type.maxDurability
        val newDur = (gear.durability - (maxDur * 0.25).toInt()).coerceAtLeast(0)
        gear.durability = newDur.toShort()
        consumeOne(voucher, player)
        if (e != null) {
            e.isCancelled = true
        }
        player.playSound(player.location, Sound.BLOCK_ANVIL_USE, 1f, 1f)
        true
    }

    /* -------------------------------------------------------------------- */
    /** Re‑rolls *all* attribute values on a gear piece while keeping quality. */
    fun statRealloc(): VoucherAction = { player, gear, voucher, e ->
        val quality = GearFactory.getQuality(gear) as Int
        GearFactory.markIdentified(gear, quality)       // rebuilds stats + lore
        consumeOne(voucher, player)
        if (e != null) {
            e.isCancelled = true
        }
        player.sendMessage("§5Attributes redistributed!")
        true
    }

    /* -------------------------------------------------------------------- */
    /** Right‑click – adds Haste I for 3 minutes. */
    fun applyHasteI(): VoucherAction  = { player, _, voucher, _ ->
        player.addPotionEffect(PotionEffect(PotionEffectType.HASTE, 3 * 60 * 20, 0))
        consumeOne(voucher, player)
        player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f)
        true
    }

    /* -------------------------------------------------------------------- */
    /** Right‑click – adds Luck II for 30 minutes (fishing boost). */
    fun applyLuckII30m(): VoucherAction = { player, _, voucher, _ ->
        player.addPotionEffect(PotionEffect(PotionEffectType.LUCK, 30 * 60 * 20, 1))
        consumeOne(voucher, player)
        player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f)
        true
    }

    /* -------------------------------------------------------------------- */
    /** Helper shared by every action. */
    private fun consumeOne(stack: ItemStack, player: Player) {
        stack.amount -= 1
        if (stack.amount <= 0) player.setItemOnCursor(ItemStack(Material.AIR))
        else player.setItemOnCursor(stack)
    }
}

/* ====================================================================== */
/**
 * Single listener that wires the VoucherSpec.customAction lambdas above into
 * the game – no more one‑off listeners needed.
 */
object VoucherActionListener : Listener {

    @EventHandler
    fun onDrag(e: InventoryClickEvent) {
        val p       = e.whoClicked as? Player ?: return
        val voucher = e.cursor ?: return
        val spec    = VoucherFactory.matchVoucher(voucher) ?: return
        val action  = spec.customAction ?: return
        val target  = e.currentItem ?: ItemStack(Material.AIR)
        if (target.type.isAir) return                  // need real target



        val success = action.invoke(p, target, voucher, e)
        if (success) p.updateInventory()
    }

    /* ------------ Right‑click self / block with voucher --------------- */
    @EventHandler
    fun onRightClick(e: PlayerInteractEvent) {
        if (e.action != Action.RIGHT_CLICK_AIR && e.action != Action.RIGHT_CLICK_BLOCK) return
        val p    = e.player
        val item = e.item ?: return
        val spec = VoucherFactory.matchVoucher(item) ?: return
        val action = spec.customAction ?: return
        if (!spec.rightClickable) return               // defined in YAML

        e.isCancelled = true
        val worked = action.invoke(p, ItemStack(Material.AIR), item, null)
        if (worked) p.inventory.removeItem(item)
    }
}
