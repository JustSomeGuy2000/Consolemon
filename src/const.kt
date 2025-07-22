package jehr.experiments.pkmnbatsim3

/**Signature all audit responder functions should use. The first Pokemon is the attacker and the second is the defender. The Any? is an original value, which the function should modify and return.*/
typealias auditFunc = (AuditData, AuditWrapper) -> Any?
/**Prints a menu and takes in user input. All checks are automatically handled. Returns true if the turn should progress, false if it should not, and null if invalid input was provided. Returning null will print a standard message, return false to avoid that and print a custom one.*/
typealias menuHandlerFunc = () -> Boolean?
/**Signature all move side functions must follow. Arguments are field, attacking pokemon, defending pokemon, and damage dealt (for recoil stuff).*/
typealias moveSideFunction = (Field, Pokemon, Pokemon, Int) -> Unit

/**Represents types themselves, without any additional information.*/
enum class Type(val shortname: String) {
    NORMAL("norm"), FIGHTING("fght"), FLYING("fly"), POISON("psn"), GROUND("grnd"), ROCK("rck"), BUG("bug"), GHOST("ghst"), STEEL("stl"), FIRE("fre"), WATER("wtr"), GRASS("grs"), ELECTRIC("elec"), PSYCHIC("psy"), DRAGON("drgn"), ICE("ice"), FAIRY("fry"), DARK("drk"), STELLAR("stlr"), TYPELESS("???")
}

/** Represents offensive matchups. That is, half damage to, double damage to, and 0 damage to. */
enum class TypeMatchup(val base: Type, val weak: List<Type> = listOf(), val strong: List<Type> = listOf(), val negate: List<Type> = listOf()) {
    NORMAL(Type.NORMAL, listOf(Type.ROCK, Type.STEEL), negate = listOf(Type.GHOST)),
    FIRE(Type.FIRE, listOf(Type.FIRE, Type.WATER, Type.ROCK, Type.DRAGON), listOf(Type.GRASS, Type.ICE, Type.BUG, Type.STEEL)),
    WATER(Type.WATER, listOf(Type.WATER, Type.GRASS, Type.DRAGON), listOf(Type.FIRE, Type.GROUND, Type.ROCK)),
    GRASS(Type.GRASS, listOf(Type.FIRE, Type.GRASS, Type.POISON, Type.FLYING, Type.BUG, Type.DRAGON, Type.STEEL), listOf(Type.WATER, Type.GROUND, Type.ROCK)),
    ELECTRIC(Type.ELECTRIC, listOf(Type.GRASS, Type.ELECTRIC, Type.DRAGON), listOf(Type.WATER, Type.FLYING), listOf(Type.GROUND)),
    ICE(Type.ICE, listOf(Type.FIRE, Type.WATER, Type.ICE, Type.STEEL), listOf(Type.GRASS, Type.GROUND, Type.FLYING, Type.DRAGON)),
    FIGHTING(Type.FIGHTING, listOf(Type.POISON, Type.FLYING, Type.PSYCHIC, Type.BUG, Type.FAIRY), listOf(Type.NORMAL, Type.ICE, Type.ROCK, Type.DARK, Type.STEEL), listOf(Type.GHOST)),
    POISON(Type.POISON, listOf(Type.POISON, Type.GROUND, Type.ROCK, Type.GHOST), listOf(Type.GRASS, Type.FAIRY), listOf(Type.STEEL)),
    GROUND(Type.GROUND, listOf(Type.GRASS, Type.BUG), listOf(Type.FIRE, Type.ELECTRIC, Type.POISON, Type.ROCK, Type.STEEL), listOf(Type.FLYING)),
    FLYING(Type.FLYING, listOf(Type.ELECTRIC, Type.ROCK, Type.STEEL), listOf(Type.GRASS, Type.FIGHTING, Type.BUG)),
    PSYCHIC(Type.PSYCHIC, listOf(Type.PSYCHIC, Type.STEEL), listOf(Type.FIGHTING, Type.POISON), listOf(Type.DARK)),
    BUG(Type.BUG, listOf(Type.FIRE, Type.FIGHTING, Type.POISON, Type.FLYING, Type.GHOST, Type.STEEL, Type.FAIRY), listOf(Type.GRASS, Type.PSYCHIC, Type.DARK)),
    ROCK(Type.ROCK, listOf(Type.FIGHTING, Type.GROUND, Type.STEEL), listOf(Type.FIRE, Type.ICE, Type.FLYING, Type.BUG)),
    GHOST(Type.GHOST, listOf(Type.DARK), listOf(Type.PSYCHIC, Type.GHOST), listOf(Type.NORMAL)),
    DRAGON(Type.DRAGON, listOf(Type.STEEL), listOf(Type.DRAGON), listOf(Type.FAIRY)),
    DARK(Type.DARK, listOf(Type.FIGHTING, Type.DARK, Type.FAIRY), listOf(Type.PSYCHIC, Type.GHOST)),
    STEEL(Type.STEEL, listOf(Type.FIRE, Type.WATER, Type.ELECTRIC, Type.STEEL), listOf(Type.ICE, Type.ROCK, Type.FAIRY)),
    FAIRY(Type.FAIRY, listOf(Type.FIRE, Type.POISON, Type.STEEL), listOf(Type.FIGHTING, Type.DRAGON, Type.DARK)),
    STELLAR(Type.STELLAR),
    TYPELESS(Type.TYPELESS);

    companion object {
        /**Get a Type's TypeMatchup object. Sure, its one line of code, but I'm afraid I'll forget it.*/
        fun getMatchup(of: Type): TypeMatchup {
            return TypeMatchup.valueOf(of.name)
        }
    }
}

/**Represents Pokemon's stat categories. All of these must be included in every Pokemon.*/
enum class Stat(val fullname: String) {
    HP("Health"), ATK("Attack"), DEF("Defense"), SPA("Special Attack"), SPD("Special Defense"), SPE("Speed"), CRTDMG("Critical Damage Multiplier"), CRTRTE("Critical Rate Multiplier"), ACC("Accuracy"), EVA("Evasion")
}

val main_stat_multipliers: Map<Int, Double> = mapOf(-6 to 0.25, -5 to 2.0/7, -4 to 1.0/3, -3 to 0.4, -2 to 0.5, -1 to 1.0/3, 0 to 1.0, 1 to 1.5, 2 to 2.0, 3 to 2.5, 4 to 3.0, 5 to 3.5, 6 to 4.0)
val acc_multipliers: Map<Int, Double> = mapOf(-6 to 1.0/3, -5 to 0.375, -4 to 3.0/7, -3 to 0.5, -2 to 0.6, -1 to 0.75, 0 to 1.0, 1 to 4.0/3, 2 to 5.0/3, 3 to 2.0, 4 to 7.0/3, 5 to 8.0/3, 6 to 3.0)
val eva_multipliers: Map<Int, Double> = mapOf(6 to 1.0/3, 5 to 0.375, 4 to 3.0/7, 3 to 0.5, 2 to 0.6, 1 to 0.75, 0 to 1.0, -1 to 4.0/3, -2 to 5.0/3, -3 to 2.0, -4 to 7.0/3, -5 to 8.0/3, -6 to 3.0)
val crtrte_multipliers: Map<Int, Double> = mapOf(0 to 1.0/24, 1 to 0.125, 2 to 0.5, 3 to 1.0)
val crtdmg_multipliers: Map<Int, Double> = mapOf(0 to 1.5)
val multiplier_ref: Map<Stat, Map<Int, Double>> = mapOf(Stat.HP to main_stat_multipliers, Stat.ATK to main_stat_multipliers, Stat.DEF to main_stat_multipliers, Stat.SPA to main_stat_multipliers, Stat.SPD to main_stat_multipliers, Stat.SPE to main_stat_multipliers, Stat.ACC to acc_multipliers, Stat.EVA to eva_multipliers, Stat.CRTDMG to crtdmg_multipliers, Stat.CRTRTE to crtrte_multipliers)

/**Represents whether a move uses physical or special stats, or if it is a status move.*/
enum class MoveType(val fullname: String) {
    PHYS("Physical"), SPEC("Special"), STATUS("Status")
}

/**Represents non-volatile statuses. These are displayed next to the Pokemon's name and are not cleared by switching out.*/
enum class NonVolatileStatus(val fullname: String, val immune: List<Type> = listOf(), val action: String) {
    PSN("Poison", listOf(Type.POISON, Type.STEEL), "poisoned"), BRN("Burn", listOf(Type.POISON), "burned"), PRZ("Paralysed", action = "paralysed"), FRZ("Frozen", listOf(Type.ICE), "frozen"), TXC("Toxic Poison", listOf(Type.POISON, Type.STEEL), "badly poisoned"), SLP("Asleep", action = "put to sleep")
}
/**Represents volatile statuses. These are not displayed and are cleared by switching out.*/
enum class VolatileStatus(val fullname: String, val remove: (Field, Pokemon) -> Unit) {
    PERISH_SONG("Perish Song", ::perishSongRemove)
}

/**Represents weather conditions.*/
enum class Weather(val fullname: String, val effects: List<AuditWrapper> = listOf()) {
    SUN("Harsh Sunlight"), RAIN("Rain"), SNOW("Snow"), HAIL("Hail"), SAND("Sandstorm"), FOG("Fog"), EX_SUN("Extremely Harsh Sunlight"), EX_RAIN("Heavy Rain"), WIND("Strong Winds"), SHADOW("Shadowy Aura")
}

/**Represents terrain conditions.*/
enum class Terrain(val fullname: String, val effects: List<AuditWrapper> = listOf()) {
    ELECTRIC("Electric Terrain"), MISTY("Misty Terrain"), GRASSY("Grassy Terrain"), PSYCHIC("Psychic Terrain")
}

/**Represents Pokemon genders.*/
enum class Gender(val fullname: String) {
    MALE("Male"), FEMALE("Female"), GENDERLESS("Genderless");

    /**Check if a gender is the opposite of yours.*/
    companion object {
        fun isOpposite(subject: Gender, obj: Gender): Boolean {
            val genderList = listOf(subject, obj)
            if (Gender.MALE in genderList && Gender.FEMALE in genderList) (return true) else (return false)
        }
    }
}

/**Represents the rates at which Pokemon can level up.*/
enum class LevellingRate(val fullname: String) {
    MEDIUM_FAST("Medium Fast"), ERRATIC("Erratic"), FLUCTUATING("Fluctuating"), MEDIUM_SLOW("Medium Slow"), FAST("Fast"), SLOW("Slow"), GLITCHED("???")
}

/**Represents the peculiar (and rather useless) quality given to Pokemon known as "Shape".*/
enum class PokemonShape(val desc: String) {
    HEAD_ONLY("Head"), HEAD_AND_TORSO("Head and torso"), SERPENTINE("Serpentine"), HEAD_AND_LEGS("Head and legs"), QUADRUPED("Quadruped"), MULTIPED("Many legs"), INSECTOID("Insectoid"), HEAD_AND_ARMS("Head and arms"), DRACONIC("Draconic"), MANY_WINGS("Many wings"), OCEANIC("Fish-like"), HUMANOID("Humanoid"), KAIJUESQUE("Kaiju-like"), MULTIPLE_BODIES("Multiple bodies")
}

/**Static information about audit responder functions. Used in enums and other global values.*/
data class AuditInfo(val event: Audit, val priority: Int, val responder: auditFunc)
/**Dynamic information about audit responder functions. Used for any non-global use of AuditInfo to prevent global alterations.*/
data class AuditWrapper(val info: AuditInfo, val owner: Any? = null, val origin: Any? = null, var extra: Map<Any, Any> = mapOf(), var time: Float = Float.POSITIVE_INFINITY) {
    fun respond(data: AuditData): Any? {
        return this.info.responder(data, this)
    }
}
/**Runtime arguments passed to audit responder functions. In an object so I can change the arguments in the future without updating all the functions.*/
data class AuditData(val field: Field?, val att: Pokemon?, val def: Pokemon?, val move: Move?, val original: Any?)

/**Represents events functions can subscribe to.*/
enum class Audit(val desc: String) {
    NEVER("Never called."), DMGCALC_WEATHER("Called when calculating the weather factor in damage calculation."), END_OF_TURN("Called at the end of the turn."), POKEMON_INIT_TYPES("Called when a Pokemon's type is being initialised."), POKEMON_CHANGE_ITEM("Called when a Pokemon's item is changed."), DMGCALC_BASE_POWER("Called when calculating the base power of a move."), MOVE_DISABLE_CHECK("Called when checking if a move should be usable or not."), DMGCALC_ACCURACY("Called when determining the accuracy of a move."), ON_SWAP_TO("Called when swapping, on the Pokemon swapped to."), DMGCALC_SET_OPP("Called when the move's target can be changed."), ON_REMOVE("Called when removed from the audit list."), DMGCALC_FINAL("Called after calculating the final damage."), GLOBAL_SOUND_CANCEL("Called when there is a chance to completely nullify sound-based moves."), PERSONAL_SOUND_CANCEL("Called when there is a chance to nullify sound-based moves for the Pokemon.")
}

fun kotlin.random.Random.randLessOrEqualInt(lessThanOrEqual: Int, max: Int = 100, min: Int = 0): Boolean {
    return (this.nextInt(min, max) <= lessThanOrEqual)
}

fun kotlin.random.Random.randLessOrEqualDouble(lessThanOrEqual: Double, max: Double = 1.0, min: Double = 0.0): Boolean {
    return (this.nextDouble(min, max) <= lessThanOrEqual)
}

var verbose: Boolean = false
fun log(message: String, prefix: String = "LOG: ", postfix: String = "") {
    if (verbose) println("$prefix$message$postfix")
}