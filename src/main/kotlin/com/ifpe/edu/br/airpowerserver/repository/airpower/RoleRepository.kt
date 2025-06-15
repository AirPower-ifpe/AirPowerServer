package com.ifpe.edu.br.airpowerserver.repository.airpower

import com.ifpe.edu.br.airpowerserver.config.RoleName
import com.ifpe.edu.br.airpowerserver.entity.airpower.Role
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface RoleRepository : JpaRepository<Role, UUID> {
    /**
     * Encontra uma Role pelo seu nome. Retorna um Optional para tratar
     * casos onde a Role pode ainda não existir no banco de dados.
     */
    fun findByName(name: RoleName): Optional<Role>
}