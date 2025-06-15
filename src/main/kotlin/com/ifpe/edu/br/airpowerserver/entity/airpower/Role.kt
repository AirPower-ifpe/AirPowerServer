package com.ifpe.edu.br.airpowerserver.entity.airpower

import com.ifpe.edu.br.airpowerserver.config.RoleName
import jakarta.persistence.*
import lombok.AllArgsConstructor
import lombok.Builder
import lombok.NoArgsConstructor
import java.util.UUID


@Entity
@Table(name = "role")
@Builder
@NoArgsConstructor
@AllArgsConstructor
class Role() {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null

    @Enumerated(EnumType.STRING)
    var name: RoleName? = null

    override fun toString(): String {
        return "Role(id=$id, name=$name)"
    }
}