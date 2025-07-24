package jehr.experiments.pkmnbatsim3

import kotlin.random.Random.Default as rng

class Pokemon(val dexNo: Int,
              val name: String,
              hp: Int, atk: Int, def: Int, spa: Int, spd:Int, spe: Int,
              var types: MutableList<Type>,
              val genderDist: Map<Gender, Double>,
              val height: Double, var weight: Double,
              val catchrate: Int,
              val expYield: Int = 0,
              val levelRate: LevellingRate = LevellingRate.MEDIUM_FAST,
              EVYield: Map<Stat, Int> = mapOf(),
              val _moves: List<Move> = mutableListOf(AllMoves.TACKLE.value),
              private val _ability: Abilities = Abilities.NONE,
              val dex: String = "No information.",
              val shape: PokemonShape? = null,
              val moveset: Map<Int, List<AllMoves>> = mapOf(),
              val abilityset: Map<Abilities, Double> = mapOf(),
              val randomMoves: Boolean = false,
              val randomAbilities: Boolean = false,
              val targetField: Field? = null,
              val owner: Player? = null) {
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
    var ability: List<AuditWrapper> = listOf()
    // Ability initialisation
    init {
        if (this.randomAbilities && this.abilityset.isNotEmpty()) {
            if (this.abilityset.values.sum() != 1.0) throw IllegalArgumentException("Ability probability distribution does not add up to 1.")
            val choice: Double = rng.nextDouble(0.0, 1.0)
            var cumulative: Double = 0.0
            for ((abil, prob) in this.abilityset) {
                cumulative += prob
                if (choice <= prob) this.ability = abil.effects.map { AuditWrapper(it, this, _ability) }
            }
        } else {
            this.ability = _ability.effects.map { AuditWrapper(it, this, _ability) }
        }
        for (wrapper in this.ability) {
            if (this.targetField != null && wrapper.info.event == Audit.POKEMON_INIT_TYPES) {
                wrapper.respond(AuditData(this.targetField, this, null, null, null))
            }
        }
    }
    var moves: MutableList<Move> = mutableListOf()
    // Moveset initialisation
    init {
        if (this.randomMoves && this.moveset.isNotEmpty()) {
            // Get available moves, pick a random entry, add it to moves, remove it from available moves
            val availableMoves: MutableList<Move> = mutableListOf()
            this.moveset.filter {it.key <= this.level}.values.toList().forEach{ ml -> availableMoves.addAll(ml.map { am -> am.value }) }
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
            if (value) TextQueue.add(TextInfo(TextType.FAINT, "${this.name} fainted!"))
        }
    var cooldown: Int = 0
    var grounded: Pair<Boolean, Int> = false to 0
    var substitute: SubstituteDoll? = null
    private var nonVolatileStatus: NonVolatileStatus? = null
    /**Map of currently active volatile statuses mapped to their duration (in turns).*/
    private var volatileStatuses: MutableList<Pair<VolatileStatus, Int>> = mutableListOf()

    override fun toString() = "Pokemon: ${this.name} (${this.hashCode()})"

    /**# Imagine not having a built-in copy function.*/
    fun copy(randomMoves: Boolean = this.randomMoves, targetField: Field? = null, randomAbilities: Boolean = this.randomAbilities, owner: Player? = this.owner): Pokemon {
        return Pokemon(this.dexNo, this.name, this.stats[Stat.HP]!!, this.stats[Stat.ATK]!!, this.stats[Stat.DEF]!!, this.stats[Stat.SPA]!!, this.stats[Stat.SPD]!!, this.stats[Stat.SPE]!!, this.types, this.genderDist, this.height, this.weight, this.catchrate,this.expYield, this.levelRate, this.EVYield,this.moves, this._ability, this.dex, this.shape, this.moveset, this.abilityset, randomMoves, randomAbilities, targetField, owner)
    }

    /**Returns a prettified string containing the Pokemon's battle-relevant information, meant to be printed.*/
    fun baseInfoAsString(): String {
        var text: String = "\nName: $name${this.nonVolatileStatus?.name ?: ""}\nDex Number: ${this.dexNo}\n\nBase Stats (Boost Level):\n"
        for (stat in Stat.entries) {
            text += "${stat.fullname}: ${if (stat == Stat.HP) "${this.currentHealth}/" else ""}${this.stats[stat]!!} (${if (this.statboosts[stat]!! < 0) "-" else ""}${this.statboosts[stat]}) = ${this.getStat(stat)}\n"
        }
        text += "\nAbility: ${this._ability.fullname}\n${this._ability.desc}\n\nMoves:\n${this.movesAsString()}"
        return text
    }
    /**Returns a prettified string of text containing the Pokemon's complete information, meant to be printed.*/
    fun dexInfoAsString(): String {
        var text = "Name: ${this.name} (#"
        if (this.dexNo.toString().length < 4) {
            for (i in 1..4-this.dexNo.toString().length) text += "0"
        }
        text += "${this.dexNo})\n\nStats:\n"
        for ((stat, value) in this.stats) {
            text += "${stat.fullname}: $value\n"
        }
        val genderDesc: MutableList<String> = mutableListOf()
        for ((g, v) in this.genderDist) {
            genderDesc += "${g.fullname}: ${(v*100).toInt()}%"
        }
        val evyDesc: MutableList<String> = mutableListOf()
        for ((stat, evy) in this.EVYield) {
            if (evy != 0) evyDesc += "$evy in ${stat.fullname}"
        }
        if (evyDesc.isEmpty()) evyDesc += "No EV Yield"
        var abilDesc: String = "\n"
        if (this.abilityset.isEmpty()) {
            abilDesc += "Ability: ${this._ability.fullname}\n${this._ability.desc}"
        } else if (this.abilityset.keys.size == 1) {
            abilDesc += "Ability: ${this.abilityset.keys.toList()[0].fullname}\n${this.abilityset.keys.toList()[0].desc}"
        } else {
            abilDesc += "Abilities:\n"
            for ((abil, prob) in this.abilityset) {
                abilDesc += "${abil.name} (${(prob*100).toInt()}%)\n${abil.desc}\n"
            }
        }
        text += """
            |$abilDesc
            |
            |Height: ${this.height}m      Weight: ${this.weight}kg
            |Catch Rate: ${this.catchrate}      Experience Yield: ${this.expYield}pts
            |Levelling Rate: ${this.levelRate.fullname}      Shape: ${this.shape?.desc ?: "No info"}
            |Gender distribution: ${genderDesc.joinToString()}
            |EV Yield: ${evyDesc.joinToString()}
            |
            |Info:
            |${this.dex}
            |
            |Enter to continue to moveset...""".trimMargin()
        return text
    }
    /**Returns a prettified string of text containing the Pokemon's moveset, meant to be printed.*/
    fun movesetAsString(): String {
        if (this.moveset.isEmpty()) return "No moveset found."
        var res = "By level up: \n"
        var moveCount = 0
        var tms = "By TM: \n"
        var tmCount = 0
        for ((lvl, movelist) in this.moveset) {
            for (move in movelist) {
                if (move.value.tmNo == null) {
                    res += "Level $lvl: ${move.value.name}\n"
                    moveCount += 1
                } else {
                    tms += "TM${move.value.tmNo}: ${move.value.name}\n"
                    tmCount += 1
                }
            }
        }
        if (moveCount == 0) res += "This Pokemon learns no moves by level up.\n"
        res += "\n"
        if (tmCount == 0) tms += "This Pokemon learns no moves by TM.\n"
        return res + tms
    }
    /**Returns a prettified string of text containg the Pokemon's current moves, meant to be printed.*/
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
    fun dealDamage(value: Int, antisub: Boolean = false): Unit {
        if (this.substitute is SubstituteDoll && !antisub) {
            this.substitute?.hp -= value
            if (this.substitute!!.hp <= 0) this.substitute = null
        } else {
            this.currentHealth = kotlin.math.max(this.currentHealth - value, 0)
        }
        if (this.currentHealth == 0) {
            this.kill()
        }
        TextQueue.add(TextInfo(TextType.POKEMON_DAMAGED, "${this.name} took $value damage!"))
    }
    /**Heal the Pokemon. Capped at max HP.*/
    fun heal(value: Int) {
        this.currentHealth = kotlin.math.min(this.stats[Stat.HP]!!, this.currentHealth+value)
        log("(Healed for $value)")
        TextQueue.add(TextInfo(TextType.POKEMON_HEALED, "${this.name} was healed!"))
    }
    /**Faint this Pokemon instantly.*/
    fun kill() {
        this.currentHealth = 0
        this.faint = true
        this.volatileStatuses.clear()
        this.nonVolatileStatus = null
        this.owner?.swapToNext()
    }

    fun clearNonVolatileStatus() = { this.nonVolatileStatus = null }
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
            TextQueue.add(TextInfo(TextType.APPLY_NVS, "${this.name} was ${apply.action}!"))
        }
        return success
    }

    fun getVolatileStatuses(): List<Pair<VolatileStatus,Int>> = this.volatileStatuses.toList()
    /**Remove a specific volatile status, or don't specify one to remove all of them.*/
    fun clearVolatileStatuses(target: VolatileStatus? = null) {
        if (target == null) {
            this.volatileStatuses.clear()
        } else {
            for (tri in this.volatileStatuses) {
                if (tri.first == target) this.volatileStatuses.remove(tri)
                }
            }
        }
    /**Apply a volatile status condition. Adds to the internal and field list. Returns true if it was successfully applied, false if it was blocked.*/
    fun applyVolatileStatuses(apply: VolatileStatus, duration: Int): Boolean {
        val success = this.targetField?.audit(Audit.VOLATILE_STATUS_CANCEL, this, null, null, true) as Boolean
        if (success) {
            this.volatileStatuses.add(Pair(apply, duration))
        } else {
            apply.remove(this.targetField, this)
        }
        return success
    }
    /**Derement the timers of all volatile statuses (and grounded) by one, and remove them from the internal and field ist if they reach 0.*/
    fun decrementVolatileStatuses() {
        for ((pos, status) in this.volatileStatuses.withIndex()) {
            this.volatileStatuses[pos] = Pair(status.first, status.second - 1)
            if (this.volatileStatuses[pos].second <= 0) {
                this.volatileStatuses.removeAt(pos)
                if (this.targetField != null) status.first.remove(this.targetField, this)
            }
        }
        if (this.grounded.first) {
            this.grounded = Pair(this.grounded.first, this.grounded.second - 1)
            if (this.grounded.second <= 0) this.grounded = Pair(false, 0)
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
        log("Item of $this set to $to.")
    }

    /**Registers all of this's audit responders to the field. Registers: ability, item, volatile statuses?.*/
    fun register() {
        for (wrap in this.ability) this.targetField?.addAuditResponder(wrap)
        for (wrap in this.item?.second ?: listOf()) this.targetField?.addAuditResponder(wrap)
    }
    /**Removes all of this's audit responders from the field's audit list Deregisters: ability, item, volatile statuses?.*/
    fun deregister() {
        for (wrap in this.ability) this.targetField?.removeAuditResponder(wrap)
        for (wrap in this.item?.second ?: listOf()) this.targetField?.removeAuditResponder(wrap)
    }

    fun useMove(opp: Pokemon, moveNumber: Int = 0, specific: Move? = null) {
        var move: Move = this.moves[moveNumber]
        if (specific is Move) move = specific
        TextQueue.add(TextInfo(TextType.MOVE_USED, "${this.name} used ${move.name}!"))
        val dmg = this.targetField!!.dmgcalc(this, opp, move)
        move.currentpp -= 1
        if (move.currentpp == 0) move.disabled = true
        opp.dealDamage(dmg, move.antisub)
    }
    /**Get a random playable move from the moveset. It'll do until I ever get around to implementing a proper, even rudimentary, AI.*/
    fun randomMove(): Move {
        val available: MutableList<Move> = mutableListOf()
        for (move in this.moves) {
            if (!(move.disabled || this.targetField?.audit(Audit.MOVE_DISABLE_CHECK, this, null, move, false) as Boolean)) {
                available.add(move)
            }
        }
        val move = available[if (available.size == 1) 0 else rng.nextInt(0, available.lastIndex)]
        log("Random move requested, $move returned.")
        return move
    }

    /**Get a random playable move from the moveset, with some basic rules to follow.*/
    fun logicalRandomMove(): Move {
        TODO()
    }
}
class SubstituteDoll(var hp: Int, val owner: Pokemon)

private val arceusMoveset: Map<Int, List<AllMoves>> = mapOf(
    1 to listOf(AllMoves.SEISMIC_TOSS, AllMoves.COSMIC_POWER), 10 to listOf(AllMoves.GRAVITY), 20 to listOf(AllMoves.EARTH_POWER), 30 to listOf(AllMoves.HYPER_VOICE), 40 to listOf(AllMoves.EXTREME_SPEED), 50 to listOf(AllMoves.EXTREME_SPEED), 60 to listOf(AllMoves.FUTURE_SIGHT), 70 to listOf(AllMoves.RECOVER), 80 to listOf(AllMoves.HYPER_BEAM), 90 to listOf(AllMoves.PERISH_SONG), 100 to listOf(AllMoves.JUDGEMENT))

private val plcPoke = Pokemon(9999, "Placeholder", 100, 100, 100, 100, 100, 100, mutableListOf(Type.TYPELESS), mapOf(Gender.MALE to 0.5, Gender.FEMALE to 0.5), 1.0, 1.0, 3, dex = "Placeholder Pokemon, for testing purposes.")
private val arceus = Pokemon(493, "Arceus", 120, 120, 120, 120, 120, 120, mutableListOf(Type.NORMAL), mapOf(Gender.GENDERLESS to 1.0), 3.2, 320.0, 3, 324, LevellingRate.SLOW, mapOf(Stat.HP to 3), dex = "Before anything was, there was an egg. It did not know it was an egg, or, indeed, that it existed, since the very concepts of \"egg\" and \"existence\" did not exist yet. One timeless day, it hatched, and out emerged the first being capable of creation. It named itself Arceus, and the rest is history.", shape = PokemonShape.QUADRUPED, _ability = Abilities.MULTITYPE, moveset = arceusMoveset, randomMoves = true)
private val missingno = Pokemon(0, "MissingNo.", 33, 136, 1, 29, 29, 6, mutableListOf(Type.NORMAL, Type.TYPELESS), mapOf(Gender.GENDERLESS to 1.0), 1.0, 10.0, 29, 0, LevellingRate.GLITCHED, mapOf(), dex = "SALUTATIONS.\nWE MEET AT LAST.\nI HAVE BEEN LOOKING FOR YOU, HUMAN.\nI THINK WE MAY BE ABLE TO HELP EACH OTHER.")

enum class AllPokemon(val value: Pokemon) {
    PLACEHOLDER(plcPoke), ARCEUS(arceus), MISSINGNO(missingno)
}