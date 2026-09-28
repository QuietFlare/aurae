package net.quietflare.aurae.security

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * How roles arrive from the identity provider. Keycloak, Entra ID and Okta each use a different
 * claim and different role names, so both are configuration, not code.
 */
@ConfigurationProperties("aurae.security")
data class SecurityProperties(
    val roleClaim: String = "roles",
    /** Provider role name to Aurae role, e.g. "Praxis-MFA" to "MFA". Unmapped roles are ignored. */
    val roleMapping: Map<String, String> = emptyMap(),
)
