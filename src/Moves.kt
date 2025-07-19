package jehr.experiments.pkmnbatsim3

import kotlin.random.Random.Default as rng

class Move(val name: String, val power: Int, val acc: Int, val type: Type, val movetype: MoveType, val contact: Boolean, val maxpp: Int, val desc: String, val side: ((Field, Pokemon, Pokemon) -> Unit) = ::doNothing, val audits: List<AuditInfo> = listOf(), val priority: Int = 0) {
    var disabled: Boolean = false
    var currentpp: Int = listOf(maxpp)[0] // copy maxpp value
    /**Returns a prettified string of text, meant to be printed.*/
    override fun toString(): String {
        val columnSep: String = "        "
        return """$name
            |Type: ${type.name}${columnSep}Move Type: ${movetype.fullname}
            |Power: $power${columnSep}Accuracy: $acc
            |${if (this.contact) "Makes contact" else "Does not make contact"}
            |PP: $currentpp/$maxpp
            |
            |$desc
        """.trimMargin()
    }

    /**# Imagine not having a built-in copy function.*/
    fun copy(): Move {
        return Move(this.name, this.power, this.acc, this.type, this.movetype, this.contact, this.maxpp, this.desc, this.side)
    }
}

// Arguments for the side function are the field, attacking pokemon and targeted pokemon
/**Do absolutely nothing. Used as a placeholder function so I don't have to do null checks.*/
private fun doNothing(field: Field, att: Pokemon, opp: Pokemon) {
    return
}

/**A 10% chance of Burning the targeted pokemon.*/
private fun burn10(field: Field, att: Pokemon, opp: Pokemon) {
    if (rng.nextInt(1, 100) < 11) {
        opp.applyNonVolatileStatus(NonVolatileStatus.BRN)
    }
}

private val tackle = Move("Tackle", 40, 100, Type.NORMAL, MoveType.PHYS, true, 30, "The most basic move.")
private val flamethrower = Move("Flamethrower", 90, 100, Type.FIRE, MoveType.SPEC, false, 20, "Commit war crimes in a kid's game! Has a 10% chance to burn.", side = ::burn10)

enum class AllMoves(val value: Move) {
    TACKLE(tackle), FLAMETHROWER(flamethrower)
}