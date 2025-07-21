package jehr.experiments.pkmnbatsim3

private fun multitypeEffect(field: Field?, owner: Pokemon?, a:Pokemon?, b:Move?, c:Any?, wrapper: AuditWrapper): Any? {
    if (owner !is Pokemon) return null
    if (owner.getItemInfo()?.first?.info == arceusPlateAuditList) {
        val type = owner.getItemInfo()?.first?.extra["type"]
        if (type is Type) {
            owner.types = mutableListOf(type)
        }
    }
    return null
}

private val multitype_info = listOf(AuditInfo(Audit.POKEMON_INIT_TYPES, 0, ::multitypeEffect), AuditInfo(Audit.POKEMON_CHANGE_ITEM, 0, ::multitypeEffect))

enum class Abilities(val fullname: String, val effects: List<AuditInfo>, val desc: String) {
    NONE("No ability", listOf(), "Nothing to see here."), MULTITYPE("Multitype", multitype_info, "Changes the Pokemon's type based on the Plate it is holding.")
}