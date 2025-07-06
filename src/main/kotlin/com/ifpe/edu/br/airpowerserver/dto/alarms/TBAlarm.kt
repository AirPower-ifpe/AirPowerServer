package com.ifpe.edu.br.airpowerserver.dto.alarms

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.ifpe.edu.br.airpowerserver.dto.Id

@JsonIgnoreProperties(ignoreUnknown = true)
data class TBAlarm(
    val id: Id,
    val createdTime: Long,
    val tenantId: Id,
    val customerId: Id,
    val type: String,
    val originator: Id,
    val severity: String,
    val acknowledged: Boolean,
    val cleared: Boolean,
    val assigneeId: Id?,
    val startTs: Long,
    val endTs: Long?,
    val ackTs: Long?,
    val clearTs: Long?,
    val assignTs: Long?,
    val propagate: Boolean,
    val propagateToOwner: Boolean,
    val propagateToTenant: Boolean,
    val propagateRelationTypes: List<String>,
    val originatorName: String?,
    val originatorLabel: String?,
    val assignee: TBAssignee?,
    val name: String,
    val status: String,
    val details: Map<String, Any> = emptyMap()
) {
    override fun toString(): String {
        return "Alarm(id=$id, createdTime=$createdTime, tenantId=$tenantId, customerId=$customerId, type='$type', originator=$originator, severity='$severity', acknowledged=$acknowledged, cleared=$cleared, assigneeId=$assigneeId, startTs=$startTs, endTs=$endTs, ackTs=$ackTs, clearTs=$clearTs, assignTs=$assignTs, propagate=$propagate, propagateToOwner=$propagateToOwner, propagateToTenant=$propagateToTenant, propagateRelationTypes=$propagateRelationTypes, originatorName=$originatorName, originatorLabel=$originatorLabel, assignee=$assignee, name='$name', status='$status', details=$details)"
    }
}