package jehr.experiments.pkmnbatsim3

import kotlin.random.Random.Default as rng

class Player(val name: String, val field: Field, team: MutableList<AllPokemon> = mutableListOf(AllPokemon.PLACEHOLDER), var selected: Int = 0) {
    var bag: MutableMap<in Item, Int> = mutableMapOf()
    var team: MutableList<Pokemon> = mutableListOf()
    init {
        for (poke in team) {
            this.team.add(poke.value.copy(targetField = this.field))
        }
    }

    /** Swap the current Pokemon with another from the team. Use position in team as argument.*/
    fun swap(to: Int) {
        if (to > this.team.lastIndex || this.team[to].faint) throw IllegalArgumentException("Attempted to access fainted pokemon or out of bounds value: $to")
        this.getSelected().clearVolatileStatuses()
        this.getSelected().deregister()
        this.selected = to
        this.getSelected().register()
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
    private val menuHandlerMap: Map<Menus, menuHandlerFunc> = mapOf(Menus.MAIN to this.menuHandler::mainMenuHandler, Menus.SWAP to this.menuHandler::swapMenuHandler, Menus.FIGHT to this.menuHandler::fightMenuHandler, Menus.INFO to this.menuHandler::infoMenuHandler, Menus.SETTINGS to this.menuHandler::settingsMenuHandler, Menus.SETTINGS_VERBOSITY to this.menuHandler::settingsVerbosityHandler)

    /**Handles everything that needs to be done at the end of the turn.*/
    fun endOfTurn() {
        this.you.decrementVolatileStatuses()
        this.opp.decrementVolatileStatuses()
        this.audit(Audit.END_OF_TURN, this, null, null, null, null)
        this.decrementAudits()
        this.turn++
        println("Current turn: ${this.turn}")
    }

    /**Puts an original value through all registered audit responder functions of a certain audit type. Returns the value unchanged if the audit type is not registered.*/
    fun audit(auditEvent: Audit, field: Field, att: Pokemon?, def: Pokemon?, move: Move?, original: Any?): Any? {
        var original = original
        for (sublist in this.audits) {
            for (wrapper in sublist) {
                if (wrapper.info.event == auditEvent) {
                    original = wrapper.respond(field, att, def, move, original)
                }
            }
        }
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
    }

    /**Remove an audit responder function from the audit list.*/
    fun removeAuditResponder(wrapper: AuditWrapper, clean: Boolean = true) {
        this.audits[wrapper.info.priority].remove(wrapper)
        if (clean) this.cleanAuditList()
    }

    /**Decrement all audit responder functions and optionally clean up the audit list.*/
    fun decrementAudits(clean: Boolean = true) {
        for (pri in this.audits) {
            for (wrapper in pri) {
                wrapper.time -= 1.0f
                if (wrapper.time <= 0.0f) pri.remove(wrapper)
            }
        }
        if (clean) this.cleanAuditList()
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
        if (to != null) {
            for (wrapper in this.terrain!!.effects) {
                this.addAuditResponder(wrapper)
            }
        }
        this.cleanAuditList()
    }

    /**Calculate a move's final damage given the context. Like real Pokemon, the return value is rounded. Unlike it, intermediate values are NOT rounded. I wonder how inaccurate that would make it?*/
    fun dmgcalc(attacker: Pokemon, defender: Pokemon, move: Move): Int {
        if (move.movetype == MoveType.STATUS) {
            move.side(this, attacker, defender)
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
        val matchup: TypeMatchup = TypeMatchup.getMatchup(move.type)
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
        move.side(this, attacker, defender)
        if (basedmg < 1 && basedmg != 0.0 && move.power > 0) (return 1) else (return kotlin.math.round(basedmg).toInt())
    }

    fun start() {
        println("You are challenged to a battle!")
        println("Your opponent sends out ${this.opp.getSelected().name}!")
        println("You send out ${this.you.getSelected().name}!\n")
        while (!this.end) {
            val nextTurn = this.menuHandlerMap[this.menu]!!()
            println()
            if (nextTurn == true) endOfTurn() else if (nextTurn == null) println("Invalid input. Try again.\n")
        }
    }
}

fun main() {
    Field.you = Player("You", Field)
    Field.opp = Player("Rival", Field)
    Field.start()
    }