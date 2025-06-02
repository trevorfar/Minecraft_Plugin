package com.trevorfarias.runic_overlord.runes
import com.trevorfarias.runic_overlord.gear.GearFactory
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.LootGenerateEvent

object RuneLootInjector : Listener {

    data class RuneLootSpec(
        val runeId: String,
        val lootTable: List<RuneLootTarget>,
        val dropChance: Float, // Base chance, scaled by luck
        val rolls: Int = 1
    )

    enum class RuneLootTarget {
        BASTION,
        VILLAGE,
        NETHER,
        END,
        ANCIENT_CITY,
        SHIPWRECK,
        GENERIC
    }



    private val registeredRuneLoots = listOf(
        RuneLootSpec("swiftness1", listOf(RuneLootTarget.BASTION, RuneLootTarget.NETHER), 0.05f),
        RuneLootSpec("swiftness2", listOf(RuneLootTarget.BASTION, RuneLootTarget.NETHER), 0.025f),
        RuneLootSpec("swiftness3", listOf(RuneLootTarget.BASTION, RuneLootTarget.NETHER), 0.01f),
        RuneLootSpec("vitality1", listOf(RuneLootTarget.BASTION, RuneLootTarget.NETHER), 0.05f),
        RuneLootSpec("vitality2", listOf(RuneLootTarget.BASTION, RuneLootTarget.NETHER), 0.025f),
        RuneLootSpec("vitality3", listOf(RuneLootTarget.BASTION, RuneLootTarget.NETHER), 0.01f),
        RuneLootSpec("luck1", listOf(RuneLootTarget.BASTION, RuneLootTarget.NETHER, RuneLootTarget.VILLAGE), 0.03f),
        RuneLootSpec("luck2", listOf(RuneLootTarget.BASTION, RuneLootTarget.NETHER, RuneLootTarget.VILLAGE), 0.01f),
        RuneLootSpec("luck3", listOf(RuneLootTarget.BASTION, RuneLootTarget.NETHER, RuneLootTarget.VILLAGE), 0.005f),
        )

    @EventHandler
    fun onLootGenerate(event: LootGenerateEvent) {
        val lootTableKey = event.lootTable.key
        val luck = event.lootContext.luck
        //event.lootTable.add(com.trevorfarias.runic_overlord.gear.GearFactory.createUnidentified("silver_sword"))

        registeredRuneLoots.forEach { spec ->
            // Flatten all targets into all their mapped tables
            val allTables = spec.lootTable.flatMap { lootTable ->
                TargetLootTableMap[lootTable] ?: emptyList()
            }

            if (lootTableKey !in allTables) return@forEach

            repeat(spec.rolls) {
                val chance = spec.dropChance + luck * 0.01f
                if (Math.random() < chance) {
                    val rune = RuneFactory.createRune(spec.runeId) ?: return@repeat
                    event.loot.add(rune)
                }
            }
        }
    }



    val TargetLootTableMap: Map<RuneLootTarget, List<NamespacedKey>> = mapOf(
        RuneLootTarget.BASTION to listOf(
            key("chests/bastion_bridge"),
            key("chests/bastion_hoglin_stable"),
            key("chests/bastion_other"),
            key("chests/bastion_treasure")
        ),
        RuneLootTarget.VILLAGE to listOf(
            key("chests/village/village_armorer"),
            key("chests/village/village_butcher"),
            key("chests/village/village_cartographer"),
            key("chests/village/village_desert_house"),
            key("chests/village/village_fisher"),
            key("chests/village/village_fletcher"),
            key("chests/village/village_mason"),
            key("chests/village/village_plains_house"),
            key("chests/village/village_savanna_house"),
            key("chests/village/village_shepherd"),
            key("chests/village/village_snowy_house"),
            key("chests/village/village_taiga_house"),
            key("chests/village/village_tanner"),
            key("chests/village/village_temple"),
            key("chests/village/village_toolsmith"),
            key("chests/village/village_weaponsmith")
        ),
        RuneLootTarget.NETHER to listOf(
            key("chests/nether_bridge"),
            key("chests/bastion_treasure"),
            key("chests/ruined_portal")
        ),
        RuneLootTarget.END to listOf(
            key("chests/end_city_treasure")
        ),
        RuneLootTarget.ANCIENT_CITY to listOf(
            key("chests/ancient_city"),
            key("chests/ancient_city_ice_box")
        ),
        RuneLootTarget.SHIPWRECK to listOf(
            key("chests/shipwreck_map"),
            key("chests/shipwreck_supply"),
            key("chests/shipwreck_treasure")
        )
    )

    private fun key(id: String): NamespacedKey = NamespacedKey.minecraft(id)

}
