package jehr.experiments.pkmnbatsim3

private fun multitype_effect(field: Field?, owner: Pokemon?, a:Pokemon?, b:Move?, c:Any?, wrapper: AuditWrapper): Any? {
    TODO()
}

private val multitype_info = listOf(AuditInfo(Audit.POKEMON_INIT_TYPES, 0, ::multitype_effect), AuditInfo(Audit.POKEMON_CHANGE_ITEM, 0, ::multitype_effect))

enum class Abilities(val fullname: String, val effects: List<AuditInfo>) {
    NONE("No ability", listOf()), MULTITYPE("Multitype", multitype_info)
}