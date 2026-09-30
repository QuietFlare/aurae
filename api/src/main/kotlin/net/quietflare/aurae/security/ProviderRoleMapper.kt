package net.quietflare.aurae.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority
import org.springframework.stereotype.Component

/**
 * At sign-in, turns the provider's role claim into Aurae roles (ROLE_MFA, ROLE_GP, ROLE_ADMIN)
 * and each role into its permissions (report:upload and so on).
 */
@Component
class ProviderRoleMapper(private val properties: SecurityProperties) : GrantedAuthoritiesMapper {

    override fun mapAuthorities(authorities: Collection<GrantedAuthority>): Collection<GrantedAuthority> {
        val auraeRoles = authorities
            .filterIsInstance<OidcUserAuthority>()
            .flatMap { it.idToken.getClaimAsStringList(properties.roleClaim).orEmpty() }
            .mapNotNull { properties.roleMapping[it] }
            .toSet()

        val permissions = auraeRoles.flatMap { permissionsOf(it) }.toSet()

        return authorities +
            auraeRoles.map { SimpleGrantedAuthority("ROLE_$it") } +
            permissions.map { SimpleGrantedAuthority(it) }
    }

    // Configuration keys may arrive lower-cased, for example from environment variables.
    private fun permissionsOf(role: String): Set<String> =
        properties.rolePermissions.entries.firstOrNull { it.key.equals(role, ignoreCase = true) }?.value.orEmpty()
}
