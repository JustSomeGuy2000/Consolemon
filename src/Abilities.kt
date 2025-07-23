package jehr.experiments.pkmnbatsim3

private fun multitypeEffect(data: AuditData, wrapper: AuditWrapper): Any? {
    if (wrapper.owner !is Pokemon) return null
    if (wrapper.owner.getItemInfo()?.first?.info == arceusPlateAuditList) {
        val type = wrapper.owner.getItemInfo()?.first?.extra["type"]
        if (type is Type) {
            wrapper.owner.types = mutableListOf(type)
        }
    }
    return null
}

private val multitype_info = listOf(AuditInfo(Audit.POKEMON_INIT_TYPES, 0, ::multitypeEffect), AuditInfo(Audit.POKEMON_CHANGE_ITEM, 0, ::multitypeEffect))

enum class Abilities(val fullname: String, val effects: List<AuditInfo>, val desc: String) {
    NONE("No ability", listOf(), "Nothing to see here."), MULTITYPE("Multitype", multitype_info, "Changes the Pokemon's type based on the Plate it is holding.");

    override fun toString() = "Ability: ${this.name} (${this.hashCode()})"
}