package com.ifpe.edu.br.airpowerserver.dto.dashboards

data class AliasFilter(
    val type: String?, // Ex: "singleEntity", "entityList", etc.
    val singleEntity: SingleEntity?, // Usado quando é um dispositivo fixo
    val entityList: List<String>? // Usado raramente para listas fixas
)
