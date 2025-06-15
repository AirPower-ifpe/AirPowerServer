package com.ifpe.edu.br.airpowerserver.repository.airpower

import com.ifpe.edu.br.airpowerserver.dto.auth.RefreshRequest
import com.ifpe.edu.br.airpowerserver.entity.airpower.PersistToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TokenRepository : JpaRepository<PersistToken, UUID> {
    fun save(token: PersistToken)
    fun findByUserIdAndScope(userId: UUID, scope: Int): PersistToken?
    fun findByRefreshToken(refreshToken: String): PersistToken?
    fun findByJwt(jwt: String): PersistToken?
}