package jehr.experiments.pkmnbatsim3

import kotlin.random.Random.Default as rng

class Pokemon(val name: String, hp: Int, atk: Int, def: Int, spa: Int, spd:Int, spe: Int, var types: MutableList<Type>, val genderDist: Map<Gender, Double>, val height: Double, var weight: Double, val catchrate: Int, val expYield: Int = 0, val levelRate: LevellingRate = LevellingRate.MEDIUM_FAST, EVYield: Map<Stat, Int> = mapOf(), val _moves: List<Move> = mutableListOf(AllMoves.TACKLE.value), private val _ability: Abilities = Abilities.NONE, val dex: String = "No information.", val shape: PokemonShape? = null, val moveset: Map<Int, Move> = mapOf(), val randomMoves: Boolean = false, val targetField: Field? = null) {
    private val stats: MutableMap<Stat, Int> = mutableMapOf(Stat.HP to hp, Stat.ATK to atk, Stat.DEF to def, Stat.SPA to spa, Stat.SPD to spd, Stat.SPE to spe, Stat.CRTDMG to 1, Stat.CRTRTE to 1, Stat.ACC to 1, Stat.EVA to 1)

    private var statboosts: MutableMap<Stat, Int> = mutableMapOf(Stat.HP to 0, Stat.ATK to 0, Stat.DEF to 0, Stat.SPA to 0, Stat.SPD to 0, Stat.SPE to 0, Stat.CRTDMG to 0, Stat.CRTRTE to 0, Stat.ACC to 0, Stat.EVA to 0)

    private var item: Pair<HeldItem, List<AuditWrapper>>? = null
    var level: Int = 100
    var exp: Int = 0
    val EVs: MutableMap<Stat, Int> = mutableMapOf(Stat.HP to 0, Stat.ATK to 0, Stat.DEF to 0, Stat.SPA to 0, Stat.SPD to 0, Stat.SPE to 0)
    val EVYield: MutableMap<Stat, Int> = mutableMapOf(Stat.HP to 0, Stat.ATK to 0, Stat.DEF to 0, Stat.SPA to 0, Stat.SPD to 0, Stat.SPE to 0)
    //EV yield initialisation
    init {
        for (stat in EVYield) {
            if (stat.key !in this.EVYield.keys) continue
            this.EVYield[stat.key] = stat.value
        }
    }
    var ability: List<AuditWrapper> = _ability.effects.map { AuditWrapper(it, this, _ability) }
    init {
        for (wrapper in this.ability) {
            if (this.targetField != null && wrapper.info.event == Audit.POKEMON_INIT_TYPES) {
                wrapper.respond(this.targetField, this, null, null, null)
            }
        }
    }
    var moves: MutableList<Move> = mutableListOf()
    // Moveset initialisation
    init {
        if (this.randomMoves && this.moveset.isNotEmpty()) {
            // Get available moves, pick a random entry, add it to moves, remove it from available moves
            val availableMoves: MutableList<Move> = this.moveset.filter {it.key <= this.level}.values.toMutableList()
            val moveCount: Int = kotlin.math.min(4, availableMoves.size)
            if (moveCount == 0) throw IllegalArgumentException("No moves available for ${this.name} at ${this.level}. Please modify moveset.")
            for (i in 1..moveCount) {
                val selected: Move = availableMoves[rng.nextInt(1, availableMoves.size)]
                this.moves.add(selected)
                availableMoves.remove(selected)
            }
            this.moves = this.moves.map {it.copy()}.toMutableList()
        } else {
            this.moves = this._moves.map {it.copy()}.toMutableList()
        }
    }
    private var currentHealth: Int = hp
    var gender: Gender = Gender.GENDERLESS
    // Gender intialisation
    init {
        if (this.genderDist.values.sum() != 1.0) throw IllegalArgumentException("Gender probability distribution for $name does not add up to 1.")
        var tempRandom: Double = rng.nextDouble(0.0, 1.0)
        var acc: Double = 0.0
        for ((g, p) in this.genderDist) {
            acc += p
            if (tempRandom <= acc) {
                this.gender = g
            }
        }
    }
    var faint: Boolean = false
        set(value) {field = value
            if (value) println("${this.name} fainted!")}
    var cooldown: Int = 0
    private var nonVolatileStatus: NonVolatileStatus? = null
    /**Map of currently active volatile statuses mapped to their duration (in turns).*/
    private var volatileStatuses: MutableMap<VolatileStatus, Int> = mutableMapOf()

    /**# Imagine not having a built-in copy function.*/
    fun copy(randomMoves: Boolean = this.randomMoves, targetField: Field? = null): Pokemon {
        return Pokemon(this.name, this.stats[Stat.HP]!!, this.stats[Stat.ATK]!!, this.stats[Stat.DEF]!!, this.stats[Stat.SPA]!!, this.stats[Stat.SPD]!!, this.stats[Stat.SPE]!!, this.types, this.genderDist, this.height, this.weight, this.catchrate,this.expYield, this.levelRate, this.EVYield,this.moves, this._ability, this.dex, this.shape, this.moveset, randomMoves, targetField)
    }

    /**Returns a prettified string of text containing the Pokemon's information, meant to be printed.*/
    fun baseInfoAsString(): String {
        var text: String = "\nName: $name${this.nonVolatileStatus?.name ?: ""}\n\nBase Stats (Boost Level):\n"
        for (stat in Stat.entries) {
            text += "${stat.fullname}: ${if (stat == Stat.HP) "${this.currentHealth}/" else ""}${this.stats[stat]!!} (${if (this.statboosts[stat]!! < 0) "-" else ""}${this.statboosts[stat]})\n"
        }
        text += "\nMoves:\n${this.movesAsString()}\n\nInfo:\n${this.dex}"
        return text
    }

    /**Returns a prettified string of text containg the Pokemon's moveset, meant to be printed.*/
    fun movesAsString(): String {
        val strList: MutableList<String> = mutableListOf()
        var strResult: String = ""
        for ((num, move) in this.moves.withIndex()) {
            strList.add("${num+1}: ${move.name}")
        }
        for ((num, str) in strList.withIndex()) {
            strResult += "$str    "
            if (num % 2 == 1) strResult += "\n"
        }
        return strResult
    }

    /** Multiply the base stat with the stat boost, found by retrieving the multiplier list and checking the multiplier for the stat's boost value */
    fun getStat(retrieve: Stat): Double {
        return this.stats[retrieve]!! * multiplier_ref[retrieve]!![this.statboosts[retrieve]!!]!!
    }

    fun getBaseStat(retrieve: Stat): Int = this.stats[retrieve]!!
    fun getStatBoostLevel(retrieve: Stat): Int = this.statboosts[retrieve]!!
    fun getCurrentHealth(): Int = this.currentHealth
    fun boostStat(stat: Stat, level: Int): Unit {
        if (level == 0) return
        val ref = this.statboosts[stat]!!
        if (level > 0) {
            this.statboosts[stat] = kotlin.math.min(6, ref + level)
        }
        if (level < 0) {
            this.statboosts[stat] = kotlin.math.max(-6, ref + level)
        }
    }
    /**Deal damage to this Pokemon. If HP reaches 0 as a result, this Pokemon faints.*/
    fun dealDamage(value: Int): Unit {
        this.currentHealth = kotlin.math.max(this.currentHealth - value, 0)
        if (this.currentHealth == 0) {
            this.faint = true
            this.volatileStatuses.clear()
            this.nonVolatileStatus = null
        }
        println("${this.name} took $value damage!")
    }

    fun getNonVolatileStatus(): NonVolatileStatus? = this.nonVolatileStatus
    /**Apply a non-volatile status condition. Returns true if it was successfully applied, false if it was blocked.*/
    fun applyNonVolatileStatus(apply: NonVolatileStatus): Boolean {
        var success: Boolean = true
        for (type in this.types) {
            if (type in apply.immune) {
                success = false
                break
            }
        }
        if (success) {
            this.nonVolatileStatus = apply
            println("${this.name} was ${apply.action}!")
        }
        return success
    }

    fun getVolatileStatuses(): Map<VolatileStatus, Int> = this.volatileStatuses.toMap()
    /**Remove a specific volatile status, or don't specify one to remove all of them.*/
    fun clearVolatileStatuses(target: VolatileStatus? = null) {
        if (target == null) {
            this.volatileStatuses.clear()
        } else {
            this.volatileStatuses.remove(target)
        }
    }
    /**Apply a volatile status condition. Returns true if it was successfully applied, false if it was blocked.*/
    fun applyVolatileStatuses(apply: VolatileStatus, duration: Int): Boolean {
        var success = true
        if (success) {
            this.volatileStatuses.put(apply, duration)
        }
        return success
    }
    /**Derement the timers of all volatile statuses by one, and remove them if they reach 0.*/
    fun decrementVolatileStatuses() {
        for ((status, time) in this.volatileStatuses) {
            this.volatileStatuses[status]?.minus(1)
            if (this.volatileStatuses[status] == 0) {
                this.volatileStatuses.remove(status)
            }
        }
    }

    fun getNameModifier(): String {
        var result: String = ""
        result += if (this.nonVolatileStatus == null) "" else "(${this.nonVolatileStatus!!.name})"
        result += if (this.faint) "(FNT)" else ""
        return result
    }
    fun getHPAsString(): String {
        return "${this.currentHealth}/${this.stats[Stat.HP]}"
    }

    fun getItemInfo(): Pair<HeldItem, List<AuditWrapper>>? {
        return this.item
    }
    fun setItem(to: HeldItem?) {
        if (this.item != null) {
            for (wrap in this.item!!.second) {
                this.targetField?.removeAuditResponder(wrap, false)
                this.targetField?.cleanAuditList()
            }
        }
        if (to != null) {
            this.item = Pair(to, to.info.map { AuditWrapper(it, this, to) })
            for (wrap in this.item!!.second) {
                this.targetField?.addAuditResponder(wrap)
            }
        } else {
            this.item = null
        }
    }

    /**Registers all of this's audit responders to the field. Registers: ability, item, volatile statuses, moves.*/
    fun register() {
        TODO()
    }
    /**Removes all of this's audit responders from the field's audit list Deregisters: ability, item, volatile statuses, moves.*/
    fun deregister() {
        TODO()
    }

    fun useMove(opp: Pokemon, moveNumber: Int) {
        val move = this.moves[moveNumber]
        println("${this.name} used ${move.name}!")
        val dmg = this.targetField!!.dmgcalc(this, opp, move)
        move.currentpp -= 1
        if (move.currentpp == 0) move.disabled = true
        opp.dealDamage(dmg)
    }
}

private val plcPoke = Pokemon("Placeholder", 100, 100, 100, 100, 100, 100, mutableListOf(Type.UNKNOWN), mapOf(Gender.MALE to 0.5, Gender.FEMALE to 0.5), 1.0, 1.0, 3, dex = "Placeholder Pokemon, for testing purposes.")
private val arceus = Pokemon("Arceus", 120, 120, 120, 120, 120, 120, mutableListOf(Type.NORMAL), mapOf(Gender.GENDERLESS to 1.0), 3.2, 320.0, 3, 324, LevellingRate.SLOW, mapOf(Stat.HP to 3), dex = "Before anything was, there was an egg. It did not know it was an egg, or, indeed, that it existed, since the very concepts of \"egg\" and \"existance\" did not exist yet. One timeless day, it hatched, and out emerged the first being capable of creation. It named itself Arceus, and the rest is history.", shape = PokemonShape.QUADRUPED)

enum class AllPokemon(val value: Pokemon) {
    PLACEHOLDER(plcPoke), ARCEUS(arceus)
}