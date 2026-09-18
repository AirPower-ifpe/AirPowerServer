package com.ifpe.edu.br.airpowerserver.dto.dashboards

data class DashboardConfiguration(
    val widgets: Map<String, Widget>?, // Pode ser nulo
    val entityAliases: Map<String, EntityAlias>? // ADICIONADO: Onde os IDs realmente vivem
)