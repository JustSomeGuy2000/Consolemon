package jehr.experiments.pkmnbatsim3

import kotlin.random.Random.Default as rng

class Player(val name: String, val field: Field, team: MutableList<AllPokemon> = mutableListOf(AllPokemon.PLACEHOLDER, AllPokemon.ARCEUS, AllPokemon.MISSINGNO
), var selected: Int = 0) {
    var bag: MutableMap<in Item, Int> = mutableMapOf()
    var team: MutableList<Pokemon> = mutableListOf()
    init {
        for (poke in team) {
            this.team.add(poke.value.copy(true, this.field, owner = this))
        }
        this.swap(selected)
    }

    override fun toString() = "Player(${this.hashCode()})"

    /** Swap the current Pokemon with another from the team. Use position in team as argument.*/
    fun swap(to: Int) {
        if (this.field.audit(Audit.BLOCK_SWAP, this.getSelected(), null, null, false) as Boolean) return
        if (to > this.team.lastIndex || this.team[to].faint) throw IllegalArgumentException("Attempted to access fainted pokemon or out of bounds value: $to")
        this.getSelected().clearVolatileStatuses()
        this.getSelected().deregister()
        println("Come back, ${this.getSelected().name}!")
        this.selected = to
        println("You're up, ${this.getSelected().name}!")
        this.field.audit(Audit.ON_SWAP_TO, null, null, null, to)
        this.getSelected().register()
    }
    /**Swap to the next Pokemon in line, or don't swap if it is the only Pokemon. Returns the swapped into Pokemon, or null, if nothing was.*/
    fun swapToNext(): Pokemon? {
        val num = this.nextPokemon()
        if (num == null) {
            return null
        } else {
            this.swap(num)
            return this.getSelected()
        }
    }

    /**Derement the timers of all volatile statuses in this player's team by one, and remove them if they reach 0.*/
    fun decrementVolatileStatuses() {
        for (poke in this.team) poke.decrementVolatileStatuses()
    }

    /**Return the position of the next unfainted Pokemon in the team. Returns null if all pokemon have fainted.*/
    fun nextPokemon(): Int? {
        var target: Pokemon
        var pointer: Int = this.selected
        for (i in 0..this.team.lastIndex) {
            pointer++
            if (pointer > this.team.lastIndex) {
                pointer = 0
            }
            target = this.team[pointer]
            if (!target.faint) return pointer
        }
        return null
    }

    /**Get the currently selected Pokemon.*/
    fun getSelected(): Pokemon {
        return this.team[this.selected]
    }

    /**Get the enemy of the player.*/
    fun getEnemy(): Player = if (this == this.field.you) this.field.opp else this.field.you

    /**Returns a prettified string of text representing the player's team, meant to be printed.*/
    fun teamToString(): String {
        var result: String = ""
        for ((pos, poke) in this.team.withIndex()) {
            result += "${pos+1}: ${poke.name} ${poke.getNameModifier()} ${if (this.getSelected() == poke) "(Selected)" else ""}\n"
        }
        return result
    }
}

object Field {
    val menuHandler = MenuHandler(this)
    /**The currently selected menu.*/
    var menu: Menus = Menus.MAIN
    var end: Boolean = false
    var turn: Int = 1
    /**The current weather. Do NOT modify directly. Use `Field.changeWeather()` instead.*/
    var weather: Weather? = null
    /**The current terrain. DO NOT modify directly. Use `Field.changeTerrain` instead.*/
    var terrain: Terrain? = null
    lateinit var you: Player
    lateinit var opp: Player
    /** A list of lists containing `AuditWrapper` objects. Audits go through priority levels of the list successively, invoking the functions of `AuditWrapper`s with specified audit events. Higher priority levels are higher numbers. Starts at priority 0.*/
    var audits: MutableList<MutableList<AuditWrapper>> = mutableListOf()
    private val menuHandlerMap: Map<Menus, menuHandlerFunc> = mapOf(Menus.MAIN to this.menuHandler::mainMenuHandler, Menus.SWAP to this.menuHandler::swapMenuHandler, Menus.FIGHT to this.menuHandler::fightMenuHandler, Menus.INFO to this.menuHandler::infoMenuHandler, Menus.SETTINGS to this.menuHandler::settingsMenuHandler, Menus.SETTINGS_VERBOSITY to this.menuHandler::settingsVerbosityHandler, Menus.DEX to this.menuHandler::dexMenuHandler)

    override fun toString(): String = "Field(${this.hashCode()})"

    /**Handles everything that needs to be done at the end of the turn.*/
    fun endOfTurn() {
        this.audit(Audit.END_OF_TURN, null, null, null, null)
        this.decrementAudits()
        this.you.decrementVolatileStatuses()
        this.opp.decrementVolatileStatuses()
        this.turn++
        println("Current turn: ${this.turn}")
    }

    /**Puts an original value through all registered audit responder functions of a certain audit type. Returns the value unchanged if the audit type is not registered.*/
    fun audit(auditEvent: Audit, att: Pokemon?, def: Pokemon?, move: Move?, original: Any?): Any? {
        log("Auditing $auditEvent with arguments : Attacker: $att, Defender: $def, Move: $move, Original value: $original")
        var original = original
        for (sublist in this.audits) {
            for (wrapper in sublist) {
                if (wrapper.info.event == auditEvent) {
                    original = wrapper.respond(AuditData(this, att, def, move, original))
                    log("Audit responder at priority level ${this.audits.indexOf(sublist)} found: $wrapper, resulting in $original.")
                }
            }
        }
        log("Audit event $auditEvent concluded with result $original.")
        return original
    }

    /**Add an audit repsonder function to the audit list. Handles all necessary creation.*/
    fun addAuditResponder(wrapper: AuditWrapper) {
        if (this.audits.lastIndex < wrapper.info.priority) {
            for (i in 1..(wrapper.info.priority-this.audits.lastIndex)) {
                this.audits.add(mutableListOf())
            }
        }
        this.audits[wrapper.info.priority].add(wrapper)
        log("Added to priority level ${wrapper.info.priority}: Audit responder $wrapper ")
    }

    /**Remove an audit responder function from the audit list.*/
    fun removeAuditResponder(wrapper: AuditWrapper, clean: Boolean = true) {
        this.audits[wrapper.info.priority].remove(wrapper)
        log("Removed from ${wrapper.info.priority}: Audit responder $wrapper ")
        if (clean) this.cleanAuditList()
    }

    /**Decrement all audit responder functions and optionally clean up the audit list.*/
    fun decrementAudits(clean: Boolean = true) {
        for (pri in this.audits) {
            for (wrapper in pri) {
                wrapper.time -= 1.0f
                if (wrapper.time <= 0.0f) {
                    pri.remove(wrapper)
                    log("Timed out and removed: Audit responder $wrapper")
                }
                if (wrapper.info.event == Audit.ON_TIMEOUT) wrapper.respond(AuditData(this, null, null, null, null))
            }
        }
        if (clean) this.cleanAuditList()
    }

    /**Look for a specific audit responder function in the audit list. Returns true if preseny, false otherwise.*/
    fun findAuditResponder(find: auditFunc): Boolean {
        for (sublist in this.audits) {
            for (wrap in sublist) {
                if (wrap.info.responder == find) return true
            }
        }
        return false
    }

    /**Goes through every audit priority level, trimming excess priotity levels.*/
    fun cleanAuditList() {
        for (i in this.audits.indices) {
            if (this.audits.last().isEmpty()) (this.audits.removeLast()) else return
        }
    }

    /**Change the weather.*/
    fun changeWeather(to: Weather?) {
        if (this.weather != null) {
            for (info in this.weather!!.effects) {
                this.removeAuditResponder(info, false)
            }
        }
        this.weather = to
        println(if (to != null) "The weather changed to ${to.fullname}" else "The weather cleared.")
        if (to != null) {
            for (info in this.weather!!.effects) {
                this.addAuditResponder(info)
            }
        }
        this.cleanAuditList()
    }

    /**Change the terrain*/
    fun changeTerrain(to: Terrain?) {
        if (this.terrain != null) {
            for (wrapper in this.terrain!!.effects) {
                this.removeAuditResponder(wrapper, false)
            }
        }
        this.terrain = to
        println(if (to != null) "The terrain changed to ${to.fullname}" else "The terrain cleared.")
        if (to != null) {
            for (wrapper in this.terrain!!.effects) {
                this.addAuditResponder(wrapper)
            }
        }
        this.cleanAuditList()
    }

    /**Calculate a move's final damage given the context. Like real Pokemon, the return value is rounded. Unlike it, intermediate values are NOT rounded. I wonder how inaccurate that would make it?*/
    fun dmgcalc(attacker: Pokemon, defender: Pokemon, move: Move): Int {
        log("Damage calculation begins with: Attacker: $attacker, defender: $defender, move: $move")
        if (move.vocal && this.audit(Audit.GLOBAL_SOUND_CANCEL, attacker, defender, move, false) as Boolean) {
            print("The move was blocked!")
            return 0
        }
        val attacker: Pokemon = this.audit(Audit.DMGCALC_SET_OPP, attacker, defender, move, attacker) as Pokemon
        if (rng.nextInt(1, 100) > this.audit(Audit.DMGCALC_ACCURACY, attacker, defender, move, move.acc) as Int && move.movetype != MoveType.STATUS) {
            println("${attacker.name} missed!")
            return 0
        }
        val transientAudits: MutableList<AuditWrapper> = mutableListOf()
        for (info in move.transients) {
            val wrap: AuditWrapper = AuditWrapper(info, attacker, move)
            this.addAuditResponder(wrap)
            transientAudits.add(wrap)
        }
        for (info in move.audits) { this.addAuditResponder(AuditWrapper(info, attacker, move)) }
        if (move.movetype == MoveType.STATUS) {
            move.side(this, attacker, defender, 0)
            for (wrap in transientAudits) {
                this.removeAuditResponder(wrap)
            }
            return 0
        }
        var basedmg: Double
        var atk: Double
        var def: Double
        val crit: Boolean = rng.randLessOrEqualDouble(attacker.getStat(Stat.CRTRTE))
        if (!crit) {
            atk = if (move.movetype == MoveType.PHYS) attacker.getStat(Stat.ATK) else attacker.getStat(Stat.SPA)
            def = if (move.movetype == MoveType.PHYS) defender.getStat(Stat.DEF) else defender.getStat(Stat.SPD)
        } else {
            atk = if (move.movetype == MoveType.PHYS) attacker.getBaseStat(Stat.ATK).toDouble() else attacker.getBaseStat(Stat.SPA).toDouble()
            def = if (move.movetype == MoveType.PHYS) defender.getBaseStat(Stat.DEF).toDouble() else defender.getBaseStat(Stat.SPD).toDouble()
        }
        basedmg = ((2 * attacker.level / 5.0 + 2) * move.power * (atk / def) / 50) + 2
        val randomFactor = rng.nextDouble(0.85, 1.0)
        val stab = if (move.type in attacker.types) 1.5 else 1.0
        val burnPenalty = if (attacker.getNonVolatileStatus() == NonVolatileStatus.BRN && move.movetype == MoveType.PHYS) 0.5 else 1.0
        val critBonus = if (crit) attacker.getStat(Stat.CRTDMG) else 1.0
        var typeEffectivenessBonus: Double = 1.0
        val matchup: TypeMatchup = TypeMatchup.getMatchup(this.audit(Audit.DMGCALC_CHANGE_MOVE_TYPE, attacker, defender, move, move.type) as Type)
        for (defType in defender.types) {
            if (defType in matchup.weak) {
                typeEffectivenessBonus *= 2
            } else if (defType in matchup.strong) {
                typeEffectivenessBonus *= 0.5
            } else if (defType in matchup.negate) {
                typeEffectivenessBonus = 0.0
                break
            }
        }
        //TIP Final damage calculation
        basedmg = basedmg * randomFactor * stab * burnPenalty * critBonus * typeEffectivenessBonus
        log("Damage breakdown: Base damage: $basedmg, Random factor: $randomFactor, STAB: $stab, Burn penalty: $burnPenalty, Crit?: $crit, Crit multiplier: $critBonus, Type effectiveness multiplier: $typeEffectivenessBonus")
        move.side(this, attacker, defender, basedmg.toInt())
        for (wrap in transientAudits) {
            this.removeAuditResponder(wrap)
        }
        if (move.delayed) {
            basedmg = 0.0
            log("Move identified as delayed. Damage nullified.")
        }
        basedmg = this.audit(Audit.DMGCALC_FINAL, attacker, defender, move, basedmg) as Double
        if (basedmg < 1 && basedmg != 0.0 && move.power > 0) (return 1) else (return kotlin.math.round(basedmg).toInt())
    }

    /**Trigger the fight phase. Does not trigger end of turn. Attacker and defender are from the point of the player.*/
    fun fight(att: Pokemon, def: Pokemon, yourMove: Move) {
        log("Fight phase begins.")
        if (att.owner == null || def.owner == null) {
            log("A Pokemon was found to be ownerless. Fight phase aborting.")
            return
        }
        val priList: MutableList<Pair<Pokemon, Move>> = mutableListOf()
        val youSpd = att.getStat(Stat.SPD)
        val oppSpd = def.getStat(Stat.SPD)
        val oppMove = def.randomMove()
        if (yourMove.priority > oppMove.priority) {
            priList.addAll(listOf(Pair(att, yourMove), Pair(def, oppMove)))
        } else if (oppMove.priority > yourMove.priority) {
            priList.addAll(listOf(Pair(def, oppMove), Pair(att, yourMove)))
        } else if (youSpd > oppSpd) {
            priList.addAll(listOf(Pair(att, yourMove), Pair(def, oppMove)))
        } else if (oppSpd > youSpd) {
            priList.addAll(listOf(Pair(def, oppMove), Pair(att, yourMove)))
        } else {
            if (rng.nextInt(1,2) == 1) {
                priList.addAll(listOf(Pair(att, yourMove), Pair(def, oppMove)))
            } else {
                priList.addAll(listOf(Pair(def, oppMove), Pair(att, yourMove)))
            }
        }
        log("Move order determined: ${priList[0]}, ${priList[1]}.")
        for ((poke, move) in priList) {
            if (poke.faint) {
                log("$poke has fainted, skipping move.")
                continue
            }
            val dmg = dmgcalc(poke, poke.owner!!.getEnemy().getSelected(), move)
            poke.useMove(poke.owner.getEnemy().getSelected(), specific = move)
            }
        log("Fight phase ends")
        this.endOfTurn()
        TextQueue.dump()
        }

    fun start() {
        println("You are challenged to a battle!")
        println("Your opponent sends out ${this.opp.getSelected().name}!")
        println("You send out ${this.you.getSelected().name}!\n")
        while (!this.end) {
            val nextTurn = this.menuHandlerMap[this.menu]!!()
            println()
            if (nextTurn == null) println("Invalid input. Try again.\n")
        }
    }
}

fun buildDex(): Map<Int, Pokemon> {
    val tempDex: MutableMap<Int, Pokemon> = mutableMapOf()
    for (poke in AllPokemon.entries) {
        tempDex.put(poke.value.dexNo, poke.value)
    }
    return tempDex.toMap()
}

val dexMap: Map<Int, Pokemon> = buildDex()

fun main() {
    Field.you = Player("You", Field)
    Field.opp = Player("Rival", Field)
    Field.start()
    }