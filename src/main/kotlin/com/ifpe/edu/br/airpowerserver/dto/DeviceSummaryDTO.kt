package com.ifpe.edu.br.airpowerserver.dto

import java.util.UUID

data class DeviceSummaryDTO(
    val id: UUID,
    val name: String,
    val label: String?,
    val type: String?,
    val isActive: Boolean
)