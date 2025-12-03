package com.ifpe.edu.br.airpowerserver.dto.dashboards

data class AliasFilter(
    val type: String?, // "relationsQuery", "entityList", etc.
    val resolveMultiple: Boolean? = false,
    val entityList: List<String>?, // Para listas manuais pequenas
    // Novos campos para suportar a Query de Relação:
    val rootEntity: SingleEntity?,
    val direction: String?, // "FROM" ou "TO"
    val relationType: String? // ex: "Contains"
)