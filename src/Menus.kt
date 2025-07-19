package jehr.experiments.pkmnbatsim3

class MenuHandler(val target: Field) {
    /**Validate user input as a menu option. Returns true if valid, false otherwise.*/
    fun validateOpt(opt: String, from: Int, to: Int, otherAllowable: List<Int> = listOf()): Boolean {
        try {
            opt.toInt()
        } catch (_: NumberFormatException) {
            return false
        }

        if (opt.toInt() in from..to || opt.toInt() in otherAllowable) (return true) else (return false)
    }

    fun mainMenuHandler(): Boolean? {
        val you = target.you.getSelected()
        val opp = target.opp.getSelected()
        val sep = "      "
        if (this.target.opp.team.all {it.faint}) {
            println("All the opponent's Pokemon have fainted. You won!")
            this.target.end = true
            return false
        }
        print("""Your Pokemon: ${you.name} ${you.getNameModifier()} ${you.getHPAsString()}
            |Opponent's Pokemon: ${opp.name} ${opp.getNameModifier()} ${opp.getHPAsString()}
            |Options:
            |1: Fight${sep}2: Swap
            |3: Info${sep}4: Run
            |5: Settings
            |> """.trimMargin())
        val opt = readln()
        if (!validateOpt(opt, 1, 4)) return null
        val option = opt.toInt()
        when (option) {
            1 -> target.menu = Menus.FIGHT
            2 -> target.menu = Menus.SWAP
            3 -> target.menu = Menus.INFO
            4 -> { this.target.end = true
                println("You fled the battle!") }
            5 -> target.menu = Menus.SETTINGS
            else -> return null
        }
        return false
    }

    fun swapMenuHandler(): Boolean? {
        println("Swap to: ")
        for ((pos, poke) in target.you.team.withIndex()) {
            println("${pos+1}: ${poke.name} ${poke.getNameModifier()}${if (target.you.selected == pos) " (Currently selected)" else ""}")
        }
        print("9: Back\n> ")
        val opt = readln()
        if (!validateOpt(opt, 1, target.you.team.size, listOf(9))) return null
        val option = opt.toInt()
        if (option == 9) {
            target.menu = Menus.MAIN
            return false
        } else if (option-1 == target.you.selected) {
            println("This pokemon is already selected.\n")
            return false
        } else if (target.you.team[option-1].faint) {
            println("This Pokemon is fainted. You cannot switch to it.\n")
            return false
        }
        target.you.swap(option-1)
        println("You swapped to ${target.you.getSelected().name}!")
        return false
    }

    fun fightMenuHandler(): Boolean? {
        val selected = target.you.getSelected()
        val moveAmount: Int = selected.moves.size
        val allowed: MutableList<Int> = mutableListOf()
        val disallowed: MutableList<Int> = mutableListOf()
        println("Selected Pokemon: ${selected.name}\nMoves:")
        for (i in 1..moveAmount) {
            println("${i}: ${selected.moves[i-1].name}${if (selected.moves[i-1].disabled) " (Cannot be chosen)".also {disallowed.add(i+1)} else ""}    ${i+moveAmount}: Move Info")
            allowed.add(i+moveAmount)
        }
        print("9: Back\n> ")
        val opt = readln()
        allowed.add(9)
        if (!this.validateOpt(opt, 1, selected.moves.size, allowed.toList())) return null
        allowed.remove(9)
        val option: Int = opt.toInt()
        when (option) {
            in allowed -> { println(selected.moves[option-moveAmount-1])
                println("Enter to continue...")
                readln()
                return false }
            in disallowed -> println("You cannot choose that move. Choose another one.").also { return false }
            9 -> this.target.menu = Menus.MAIN
            else -> { selected.useMove(this.target, this.target.opp.getSelected(), option-1)
                this.target.menu = Menus.MAIN
                return true }
        }
        return false
    }

    fun infoMenuHandler(): Boolean? {
        print("""What would you like info on?
            |${target.you.teamToString()}
            |7: Opponent's selected Pokemon (${target.opp.getSelected().name})
            |8: Back
            |> 
        """.trimMargin())
        val option: String = readln()
        if (!this.validateOpt(option, 1, target.you.team.size, listOf(7, 8))) return null
        val opt: Int = option.toInt()
        when (opt) {
            in 1..target.you.team.size -> println(target.you.team[opt-1]).also { print("Enter to continue..."); readln() }
            7 -> println(target.opp.getSelected()).also { print("Enter to continue..."); readln() }
            8 -> target.menu = Menus.MAIN
        }
        return false
    }

    fun settingsMenuHandler(): Boolean? {
        TODO()
    }
}

enum class Menus(val fullname: String) {
    MAIN("Main Menu"), SWAP("Swap Menu"), FIGHT("Fight Menu"), INFO("Info Menu"), SETTINGS("Settings Menu")
}