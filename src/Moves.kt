package jehr.experiments.pkmnbatsim3

import kotlin.math.roundToInt
import kotlin.random.Random.Default as rng

class Move(val name: String,
           val power: Int,
           val acc: Int,
           val type: Type,
           val movetype: MoveType,
           val contact: Boolean,
           val maxpp: Int,
           val desc: String,
           /**A function executed once, just before damage calculation ends.*/
           val side: moveSideFunction = ::doNothing,
           /**`AuditInfo`s wrapped and added to the audit list just before damage calculation and not removed.*/
           val audits: List<AuditInfo> = listOf(),
           val priority: Int = 0,
           /**`AuditInfo`s wrapped and added to the audit list just before the damage calculation starts, and removed just before it ends. The main method of influencing damage calculation.*/
           val transients: List<AuditInfo> = listOf(),
           val vocal: Boolean = false,
           val antisub: Boolean = false,
           val delayed: Boolean = false,
           val tmNo: Int? = null) {
    var disabled: Boolean = false
    var currentpp: Int = listOf(maxpp)[0] // copy maxpp value

    override fun toString() = "Move: ${this.name} (${this.hashCode()})"

    /**Returns a prettified string of text, meant to be printed.*/
    fun moveInfoAsString(): String {
        val columnSep: String = "        "
        return """$name
            |Type: ${type.name}${columnSep}Move Type: ${movetype.fullname}
            |Power: ${if (this.power >= 0) this.power else "--"}${columnSep}Accuracy: ${if (this.movetype == MoveType.STATUS) "--" else this.acc}%
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
private fun doNothing(field: Field, att: Pokemon, opp: Pokemon, dmg: Int)  = Unit
/**Ground the Pokemon given as the attacker (usually the Pokemon switched in) until the wrapper's timer is up.*/
private fun ground(data: AuditData, wrapper: AuditWrapper) {
    data.att?.grounded = Pair(true, wrapper.time.toInt())
}
/**A 10% chance of Burning the targeted pokemon.*/
private fun burn10(field: Field, att: Pokemon, opp: Pokemon, dmg: Int) {
    if (rng.nextInt(1, 100) < 11) {
        opp.applyNonVolatileStatus(NonVolatileStatus.BRN)
    }
}
/**A 10% chance of lowering the opponen'ts Sp.D. by one stage.*/
private fun spdminus1stage10(field: Field, att: Pokemon, opp: Pokemon, dmg: Int) {
    if (rng.nextInt(1, 100) < 11) {
        opp.boostStat(Stat.SPD, -1)
    }
}
/**Deal damage to the attacker equal to 25% of its health.*/
private fun recoil25hp(field: Field, att: Pokemon, opp: Pokemon, dmg: Int) {
    att.dealDamage((att.getBaseStat(Stat.HP)*0.25).toInt())
}
/**Inflict damage according to the attacker's level*/
private fun seismicTossDmg(data: AuditData, wrapper: AuditWrapper): Any? = data.att?.level ?: 0
/**Raise the user's defense and special defense by 1 stage.*/
private fun cosmicPowerEffect(field: Field, att: Pokemon, def: Pokemon, dmg: Int) {
    att.boostStat(Stat.DEF, 1)
    att.boostStat(Stat.SPD, 1)
}
private fun gravityAccuracyBoost(data: AuditData, wrap: AuditWrapper) = data.original as Int * (6840/4096)
private val gravityDisableList: List<Move> = listOf()
private fun gravityMoveDisable(data: AuditData, wrap: AuditWrapper) = data.move in gravityDisableList
/**Wrap and register all of `Gravity`'s `AuditInfo`s*/
private fun gravityRegister(field: Field, att: Pokemon, def: Pokemon, dmg: Int) {
    field.addAuditResponder(AuditWrapper(AuditInfo(Audit.DMGCALC_ACCURACY, 0, ::gravityAccuracyBoost), att, gravity, time = 5.0f))
    field.addAuditResponder(AuditWrapper(AuditInfo(Audit.MOVE_DISABLE_CHECK, 0, ::gravityMoveDisable), att, gravity, time = 5.0f))
    field.you.getSelected().grounded = Pair(true, 5)
    field.opp.getSelected().grounded = Pair(true, 5)
    field.addAuditResponder(AuditWrapper(AuditInfo(Audit.ON_SWAP_TO, 0, ::ground), att, gravity, time = 5.0f))
}/**CLear the target of status conditions and heal them to full if they are hurt or have conditions.*/
private fun healingWishEffect(data: AuditData, wrap: AuditWrapper) {
    val target: Pokemon? = data.original as? Pokemon
    if (target == null) return
    if (data.field == null || data.field.findAuditResponder(::healingWishEffect)) return
    if (target.getStat(Stat.HP).toInt() != target.getCurrentHealth() || target.getNonVolatileStatus() != null || target.getVolatileStatuses().isEmpty()) {
        target.heal(target.getBaseStat(Stat.HP))
        target.clearVolatileStatuses()
        target.clearNonVolatileStatus()
        data.field.removeAuditResponder(wrap)
    }
}
/**# You should kill yourself! NOW!*/
private fun youShouldKillYouselfNow(field: Field, att: Pokemon, def: Pokemon, dmg: Int) = att.kill()
private fun futureSightExecute(data: AuditData, wrap: AuditWrapper) {
    val att: Pokemon? = wrap.extra["att"] as? Pokemon
    if (att !is Pokemon) return
    if (data.field !is Field) return
    val dmg = data.field.dmgcalc(att, att.owner!!.getEnemy().getSelected(), futureSight)
    att.owner.getEnemy().getSelected().dealDamage(dmg)
}
private fun futureSightPrepare(field: Field, att: Pokemon, def: Pokemon, dmg: Int) { field.addAuditResponder(AuditWrapper(AuditInfo(Audit.ON_TIMEOUT, 0, ::futureSightExecute), att, futureSight, mapOf("att" to att), 2.0f)) }
private fun recoverHeal(field: Field, att: Pokemon, def: Pokemon, dmg: Int) { att.heal(att.getStat(Stat.HP).roundToInt()) }
private fun recharge1After(field: Field, att: Pokemon, def: Pokemon, dmg: Int) { att.cooldown = 1 }
private fun perishSongExecute(data: AuditData, wrap: AuditWrapper) {
    val target = wrap.extra["target"] as Pokemon
    target.kill()
}
private fun perishSongPrepare(field: Field, att: Pokemon, def: Pokemon, dmg: Int) {
    field.addAuditResponder(AuditWrapper(AuditInfo(Audit.ON_TIMEOUT, 0, ::perishSongExecute), att, perishSong, extra = mapOf("target" to field.you.getSelected()), time = 3.0f))
    att.applyVolatileStatuses(VolatileStatus.PERISH_SONG, 3)
    if (!(field.audit(Audit.PERSONAL_SOUND_CANCEL, att, def, perishSong, false) as Boolean)) {
        field.addAuditResponder(AuditWrapper(AuditInfo(Audit.ON_TIMEOUT, 0, ::perishSongExecute), def, perishSong, extra = mapOf("target" to att.owner!!.getEnemy().getSelected()), time = 3.0f))
        def.applyVolatileStatuses(VolatileStatus.PERISH_SONG, 3)
    }
}
fun perishSongRemove(field: Field, target: Pokemon) {
    for (level in field.audits) {
        for (wrap in level) {
            if (wrap.owner == target && wrap.origin == perishSong) field.removeAuditResponder(wrap)
        }
    }
}
fun judgementChangeType(data: AuditData, wrap: AuditWrapper): Type {
    val item = data.att?.getItemInfo()?.first
    if (item == null || item.info != arceusPlateAuditList) return Type.NORMAL
    return (item.extra["type"] as? Type) ?: Type.NORMAL
}

private val tackle = Move("Tackle", 40, 100, Type.NORMAL, MoveType.PHYS, true, 30, "The most basic move.")
private val flamethrower = Move("Flamethrower", 90, 100, Type.FIRE, MoveType.SPEC, false, 20, "Commit war crimes in a kid's game! Has a 10% chance to burn.", side = ::burn10)
private val seismicToss = Move("Seismic Toss", -1, 100, Type.FIGHTING, MoveType.PHYS, true, 20, "Manipulate gravity to squish the opponent into the ground. Deals damage equal to the user's level for some reason.", transients = listOf(AuditInfo(Audit.DMGCALC_BASE_POWER, 0, ::seismicTossDmg)))
private val cosmicPower = Move("Cosmic Power", -1, 100, Type.PSYCHIC, MoveType.STATUS, false, 20, "Absorb some sort of space power to boost your defense and special defense.", ::cosmicPowerEffect)
private val gravity = Move("Gravity", -1, 100, Type.PSYCHIC, MoveType.STATUS, false, 5, "Enhance gravity for 5 turns, causing all sorts of battlefield effects.", side = ::gravityRegister)
private val struggle = Move("Struggle", 50, 100, Type.TYPELESS, MoveType.PHYS, true, 1, "So, it has come to this, then? Good luck, soldier.", side = ::recoil25hp)
private val earthPower = Move("Earth Power", 90, 100, Type.GROUND, MoveType.SPEC, false, 10, "Make the ground under the opponent erupt with sudden energy. Has a 10% chance to lower their Sp.D. by 1 stage.", side = ::spdminus1stage10)
private val hyperVoice = Move("Hyper Voice", 90, 100, Type.NORMAL, MoveType.SPEC, false, 10, "An earsplitting screech that pierces through Substitutes.", antisub = true, vocal = true)
private val extremeSpeed = Move("Extreme Speed", 80, 100, Type.NORMAL, MoveType.PHYS, true, 5, "No, its not Swift.", priority = 2)
private val healingWish = Move("Healing Wish", -1, 100, Type.PSYCHIC, MoveType.STATUS, false, 10, "The epitome of useless heroic scrifices. Kill yourself but ensure the next Pokemon that comes in is healed to full and cleared of all status conditions.", side = ::youShouldKillYouselfNow, audits = listOf(AuditInfo(Audit.ON_SWAP_TO, 0, ::healingWishEffect)))
private val futureSight = Move("Future Sight", 120, 100, Type.PSYCHIC, MoveType.SPEC, false, 10, "Foresee an attack in the future. And execute it, since its your attack apparently. Self-fulfilling prophecy or predestination?", side = ::futureSightPrepare)
private val recover = Move("Recover", -1, 100, Type.NORMAL, MoveType.STATUS, false, 5, "Spontaneously induce rapid cell growth to recover (he said the thing!) 50% of yout HP.", side = ::recoverHeal)
private val hyperBeam = Move("Hyper Beam", 150, 90, Type.NORMAL, MoveType.SPEC, false, 5, "Fire a devastating laser at the opponent all anime-style. Requires one turn of recharge.", side = ::recharge1After)
private val perishSong = Move("Perish Song", -1, 100, Type.NORMAL, MoveType.STATUS, false, 5, "Sing a malevolent song. In 3 turns, all who have heard it and are still afflicted with it will die on the spot.", side = ::perishSongPrepare, vocal = true)
private val judgement = Move("Judgement", 100, 100, Type.NORMAL, MoveType.SPEC, false, 10, "Call upon the power of Arceus itself to rain destruction down on opponents.", transients = listOf(AuditInfo(Audit.DMGCALC_CHANGE_MOVE_TYPE, 0, ::judgementChangeType)))

enum class AllMoves(val value: Move) {
    TACKLE(tackle), FLAMETHROWER(flamethrower), SEISMIC_TOSS(seismicToss), COSMIC_POWER(cosmicPower), GRAVITY(gravity), STRUGGLE(struggle), EARTH_POWER(earthPower), HYPER_VOICE (hyperVoice), EXTREME_SPEED(extremeSpeed), HEALING_WISH(healingWish), FUTURE_SIGHT(futureSight), RECOVER(recover), HYPER_BEAM(hyperBeam), PERISH_SONG(perishSong), JUDGEMENT(judgement)
}