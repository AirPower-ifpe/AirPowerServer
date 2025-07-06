package com.ifpe.edu.br.airpowerserver.dto.alarms

import com.ifpe.edu.br.airpowerserver.dto.Id

data class APAlarmInfo(
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
    val originatorName: String?,
    val originatorLabel: String?,
    val assignee: TBAssignee?,
    val name: String,
    val status: String
) {
    override fun toString(): String {
        return "APAlarmInfo(id=$id, " +
                "createdTime=$createdTime, " +
                "tenantId=$tenantId, " +
                "customerId=$customerId, " +
                "type='$type'," +
                " originator=$originator, " +
                "severity='$severity'," +
                " acknowledged=$acknowledged, " +
                "cleared=$cleared, " +
                "assigneeId=$assigneeId, " +
                "originatorName=$originatorName," +
                " originatorLabel=$originatorLabel," +
                " assignee=$assignee, " +
                "name='$name', " +
                "status='$status')"
    }
}