package jehr.experiments.pkmnbatsim3

open class Item()
data class HeldItem(val name: String, val info: List<AuditInfo>, val desc: String = "No information.", val extra: MutableMap<Any, Any> = mutableMapOf()): Item()
data class UsableItem(val name: String, val info: auditFunc, val desc: String = "No information.", val extra: MutableMap<Any, Any> = mutableMapOf()): Item()

val arceusPlateAuditList = listOf(AuditInfo(Audit.NEVER, 0, ::arceusPlatesTypeChange), AuditInfo(Audit.DMGCALC_BASE_POWER, 0, ::arceusPlatesDMGBoost))

private fun doNothing(field: Field?, poke1: Pokemon?, poke2: Pokemon?, move: Move?, org: Any?, wrap: AuditWrapper): Any? = null
private fun arceusPlatesTypeChange(field: Field?, poke1: Pokemon?, poke2: Pokemon?, move: Move?, org: Any?, wrap: AuditWrapper): Any? = null
private fun arceusPlatesDMGBoost(field: Field?, poke1: Pokemon?, poke2: Pokemon?, move: Move?, org: Any?, wrap: AuditWrapper): Any? = null

private val plcItem = HeldItem("Placeholder", listOf(AuditInfo(Audit.NEVER, 0, ::doNothing)))
private val blankPlate = HeldItem("Blank Plate", arceusPlateAuditList, "A stone tablet imbued with the essence of normalcy. With it comes the power to secure, contain and protect.", mutableMapOf("type" to Type.NORMAL))
private val fistPlate = HeldItem("Fist Plate", arceusPlateAuditList, "A stone tablet imbued with the essence of fighting spirit. With it comes the capability for wrath, whether for ill or for good.", mutableMapOf("type" to Type.FIGHTING))
private val skyPlate = HeldItem("Sky Plate", arceusPlateAuditList, "A stone tablet imbued with the essence of flight. With it comes the concept of freedom, and all the horror and beauty it can entail.", mutableMapOf("type" to Type.FLYING))
private val toxicPlate = HeldItem("Toxic Plate", arceusPlateAuditList, "A stone tablet imbued with the essence of poison. With it comes the capability for hatred, the great destroyer.", mutableMapOf("type" to Type.POISON))
private val earthPlate = HeldItem("Earth Plate", arceusPlateAuditList, "A stone tablet imbued with the essence of the earth. With it comes the power to create and nurture all forms of life.", mutableMapOf("type" to Type.GROUND))
private val stonePlate = HeldItem("Stone Plate", arceusPlateAuditList, "A stone tablet imbued with the essence of rock. With it comes the concept of stability, the foundation on which all that is lawful stands.", mutableMapOf("type" to Type.ROCK))
private val insectPlate = HeldItem("Toxic Plate", arceusPlateAuditList, "A stone tablet imbued with the essence of bugs. With it comes the concept of work, the substrate of life and death alike.", mutableMapOf("type" to Type.BUG))
private val spookyPlate = HeldItem("Spooky Plate", arceusPlateAuditList, "A stone tablet imbued with the essence of ghosts. With it comes the unceasing drive of all beings for legacy, and the equally unceasing agony of those beings to achieve it.", mutableMapOf("type" to Type.GHOST))
private val ironPlate = HeldItem("Iron Plate", arceusPlateAuditList, "A stone tablet imbued with the essence of steel. With it comes the capability for industry, the driving force behind many joys and sorrows.", mutableMapOf("type" to Type.STEEL))
private val flamePlate = HeldItem("Flame Plate", arceusPlateAuditList, "A stone tablet imbued with the essence of fire. With it comes the power to burn, which is to destroy, but also to create anew.", mutableMapOf("type" to Type.FIRE))
private val splashPlate = HeldItem("Splash Plate", arceusPlateAuditList, "A stone tablet imbued with the essence of water. With it comes the power to wash clean a piece of crockery or the entire earth.", mutableMapOf("type" to Type.WATER))
private val meadowPlate = HeldItem("Meadow Plate", arceusPlateAuditList, "A stone tablet imbued with the essence of plants. With it comes the concept of beginnings, from which all things arise.", mutableMapOf("type" to Type.GRASS))
private val zapPlate = HeldItem("Zap Plate", arceusPlateAuditList, "A stone tablet imbued with the essence of electricity. With it comes the concept of drive, the invisible, indispensable impetus that propels all beings to do.", mutableMapOf("type" to Type.ELECTRIC))
private val mindPlate = HeldItem("Mind Plate", arceusPlateAuditList, "A stone tablet imbued with the essence of psychic energy. With it comes the concept of the mind, a machine capable of breathing light into the world, as well as smothering it in darkness.", mutableMapOf("type" to Type.PSYCHIC))
private val iciclePlate = HeldItem("Icicle Plate", arceusPlateAuditList, "A stone tablet imbued with the essence of ice. With it comes the concept of eternity, pursued by gods and mortals alike.", mutableMapOf("type" to Type.ICE))
private val dracoPlate = HeldItem("Draco Plate", arceusPlateAuditList, "A stone tablet imbued with the essence of dragons. With it comes the concept of the unknown, the progenitor of fear.", mutableMapOf("type" to Type.DRAGON))
private val dreadPlate = HeldItem("Dread Plate", arceusPlateAuditList, "A stone tablet imbued with the essence of darkness. With it comes the capability for evil, all the ugliness and darkness in the world.", mutableMapOf("type" to Type.DARK))
private val pixiePlate = HeldItem("Pixie Plate", arceusPlateAuditList, "A stone tablet imbued with the essence of fairies. WIth it comes the capability for good, all the beauty and light in the world.", mutableMapOf("type" to Type.FAIRY))
private val legendPlate = HeldItem("Legend Plate", arceusPlateAuditList, "A stone tablet imbued with the essence of all creation. WIth it comes the concept of existence. All that is is partly because of this.", mutableMapOf("type" to "null"))

/**Represents all items holdable by Pokemon.*/
enum class AllHeldItems(value: HeldItem) {
    PLCITEM(plcItem), BLANK_PLATE(blankPlate), FIST_PLATE(fistPlate), SKY_PLATE(skyPlate), TOXIC_PLATE(toxicPlate), EARTH_PLATE(earthPlate), STONE_PLATE(stonePlate), INSECT_PLATE(insectPlate), SPOOKY_PLATE(spookyPlate), IRON_PLATE(ironPlate), FLAME_PLATE(flamePlate), SPLASH_PLATE(splashPlate), MEADOW_PLATE(meadowPlate), ZAP_PLATE(zapPlate), MIND_PLATE(mindPlate), ICICLE_PLATE(iciclePlate), DRACO_PLATE(dracoPlate), DREAD_PLATE(dreadPlate), PIXIE_PLATE(pixiePlate), LEGEND_PLATE(legendPlate)
}
/**Represents all items that are used immediately.*/
enum class AllUsableItems(value: UsableItem)