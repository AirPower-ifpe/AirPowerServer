package com.ifpe.edu.br.airpowerserver.entity.airpower

import jakarta.persistence.*
import lombok.AllArgsConstructor
import lombok.Builder
import lombok.NoArgsConstructor
import java.util.UUID


@Table(name = "users")
@Entity(name = "User")
@NoArgsConstructor
@AllArgsConstructor
@Builder
class AirPowerUser {
    @Id
    @Column(unique = true)
    var id: UUID? = null

    @Column(unique = true)
    var email: String? = null

    var password: String? = null

    @ManyToOne(fetch = FetchType.EAGER, cascade = [CascadeType.ALL])
    @JoinTable(
        name = "users_roles",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "role_id")]
    )
    var role: Role? = null

    override fun toString(): String {
        return "AirPowerUser(id=$id, email=$email, password=$password, roles=$role)"
    }
}