package com.ifpe.edu.br.airpowerserver.dto.dashboards

data class EntityAlias(
    val id: String,
    val alias: String,
    val filter: AliasFilter?
)
