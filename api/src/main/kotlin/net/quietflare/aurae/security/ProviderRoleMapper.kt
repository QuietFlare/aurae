package net.quietflare.aurae.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority
import org.springframework.stereotype.Component

/** Turns the provider's role claim into Aurae roles (ROLE_MFA, ROLE_GP, ROLE_ADMIN) at sign-in. */
@Component
class ProviderRoleMapper(private val properties: SecurityProperties) : GrantedAuthoritiesMapper {

    override fun mapAuthorities(authorities: Collection<GrantedAuthority>): Collection<GrantedAuthority> {
        val providerRoles = authorities
            .filterIsInstance<OidcUserAuthority>()
            .flatMap { it.idToken.getClaimAsStringList(properties.roleClaim).orEmpty() }

        val auraeRoles = providerRoles
            .mapNotNull { properties.roleMapping[it] }
            .map { SimpleGrantedAuthority("ROLE_$it") }

        return authorities + auraeRoles
    }
}
