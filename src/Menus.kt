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
        } else if (this.target.you.team.all {it.faint}) {
            println("ALl your Pokemon have fainted. You blacked out.")
            this.target.end = true
            return false
        }
        print("""Your Pokemon: ${you.name} ${you.getNameModifier()} ${you.getHPAsString()}
            |Opponent's Pokemon: ${opp.name} ${opp.getNameModifier()} ${opp.getHPAsString()}
            |Options:
            |1: Fight${sep}2: Swap
            |3: Info${sep}4: Run
            |5: Settings${sep}6: Dex
            |> """.trimMargin())
        val opt = readln().trim()
        if (!validateOpt(opt, 1, 6)) return null
        val option = opt.toInt()
        when (option) {
            1 -> target.menu = Menus.FIGHT
            2 -> target.menu = Menus.SWAP
            3 -> target.menu = Menus.INFO
            4 -> { this.target.end = true
                println("You fled the battle!") }
            5 -> target.menu = Menus.SETTINGS
            6 -> target.menu = Menus.DEX
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
        val opt = readln().trim()
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
        var struggle: Boolean = false
        println("Selected Pokemon: ${selected.name}\nMoves:")
        for (i in 1..moveAmount) {
            println("${i}: ${selected.moves[i-1].name}${if (selected.moves[i-1].disabled || this.target.audit(Audit.MOVE_DISABLE_CHECK, selected, null, selected.moves[i-1], false) as Boolean) " (Cannot be chosen)".also {disallowed.add(i+1)} else ""}    ${i+moveAmount}: Move Info")
            allowed.add(i+moveAmount)
        }
        if (disallowed.size == moveAmount) {
            struggle = true
            println("10: Struggle")
        }
        print("9: Back\n> ")
        val opt = readln().trim()
        allowed.add(9)
        if (struggle) allowed.add(10)
        if (!this.validateOpt(opt, 1, selected.moves.size, allowed.toList())) return null
        if (struggle) allowed.remove(10)
        allowed.remove(9)
        val option: Int = opt.toInt()
        when (option) {
            in allowed -> { println(selected.moves[option-moveAmount-1].moveInfoAsString())
                println("Enter to continue...")
                readln()
                return false }
            in disallowed -> println("You cannot choose that move. Choose another one.").also { return false }
            9 -> this.target.menu = Menus.MAIN
            10 -> this.target.fight(selected, this.target.opp.getSelected(), AllMoves.STRUGGLE.value)
            else -> { this.target.fight(selected, this.target.opp.getSelected(), selected.moves[option-1])
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
        val option: String = readln().trim()
        if (!this.validateOpt(option, 1, target.you.team.size, listOf(7, 8))) return null
        val opt: Int = option.toInt()
        when (opt) {
            in 1..target.you.team.size -> println(target.you.team[opt-1].baseInfoAsString()).also { print("Enter to continue...")
                readln() }
            7 -> println(target.opp.getSelected().baseInfoAsString()).also { print("Enter to continue...")
                readln() }
            8 -> target.menu = Menus.MAIN
        }
        return false
    }

    fun settingsMenuHandler(): Boolean? {
        print("""Change settings:
            |1. Verbosity: ${if (Env.settings.verbose) "On" else "Off"}
            |9. Back
            |> 
        """.trimMargin())
        val option: String = readln().trim()
        if (!this.validateOpt(option, 1, 1, listOf(9))) return null
        val opt: Int = option.toInt()
        when (opt) {
            1 -> this.target.menu = Menus.SETTINGS_VERBOSITY
            9 -> this.target.menu = Menus.MAIN
        }
        return false
    }

    fun settingsVerbosityHandler(): Boolean? {
        print("""Change verbosity:
            |1. On      2. Off
            |3. Back
            |> 
        """.trimMargin())
        val option: String = readln().trim()
        if (!this.validateOpt(option, 1, 3)) return null
        val opt: Int = option.toInt()
        when (opt) {
            1 -> Env.settings.verbose = true
            2 -> Env.settings.verbose = false
            3 -> {}
        }
        this.target.menu = Menus.SETTINGS
        return false
    }

    fun dexMenuHandler(): Boolean? {
        print("Enter dex number or Pokemon name> ")
        val opt = readln().trim()
        var selected: Pokemon?
        var isName: Boolean = false
        try {
            opt.toInt()
        } catch (_: NumberFormatException) {
            try {
                AllPokemon.valueOf(opt.uppercase()).value
                isName = true
            } catch (_: IllegalArgumentException) {
                print("Pokemon not found.")
                return false
            }
        }
        if (!isName) {
            val option: Int = opt.toInt()
            selected = dexMap[option]
        } else {
            selected = AllPokemon.valueOf(opt.uppercase()).value
        }
        if (selected == null) {
            println("Pokemon not found")
            return false
        }
        println(selected.dexInfoAsString())
        readln()
        println(selected.movesetAsString())
        readln()
        this.target.menu = Menus.MAIN
        return false
    }
}

class EnvMenuHandler(val target: Env) {
    fun startMenuHandler() {
        print("""
            |                    WELCOME         TO
            |   _____                      _                            
            |  / ____|                    | |                           
            | | |     ___  _ __  ___  ___ | | ___ _ __ ___   ___  _ __  
            | | |    / _ \| '_ \/ __|/ _ \| |/ _ \ '_ ` _ \ / _ \| '_ \ 
            | | |___| (_) | | | \__ \ (_) | |  __/ | | | | | (_) | | | |
            |  \_____\___/|_| |_|___/\___/|_|\___|_| |_| |_|\___/|_| |_|
            |               
            |1. START GAME          2. CONFIGURE TEAM
            |3. POKEDEX             4. ITEM DEX
            |5. SETTINGS            6. QUIT
            |INPUT OPTION >>> """.trimMargin())
        val opt = readln()
        when (opt) {
            "1" -> this.target.startGame()
            "2" -> TODO()
            "3" -> TODO()
            "4" -> TODO()
            "5" -> TODO()
            "6" -> {
                println("Good day.")
                this.target.quit = true
            }
            else -> println("Invalid input, please try again.")
        }
    }
}

enum class Menus(val fullname: String) {
    MAIN("Main Menu"), SWAP("Swap Menu"), FIGHT("Fight Menu"), INFO("Info Menu"), SETTINGS("Settings Menu"), SETTINGS_VERBOSITY("Verbosity Settings Menu"), DEX("Pokedex CLI"), START("Start Menu")
}