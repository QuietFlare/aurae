package net.quietflare.aurae.security

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * How roles arrive from the identity provider and what each role may do. Keycloak, Entra ID and
 * Okta each use a different claim and different role names, so all of this is configuration.
 */
@ConfigurationProperties("aurae.security")
data class SecurityProperties(
    val roleClaim: String = "roles",
    /** Provider role name to Aurae role, e.g. "Praxis-MFA" to "MFA". Unmapped roles are ignored. */
    val roleMapping: Map<String, String> = emptyMap(),
    /** Aurae role to its permissions, see [Permission]. A role without an entry gets none. */
    val rolePermissions: Map<String, Set<String>> = emptyMap(),
    /**
     * Ask the provider for the password on every sign-in, even if it still has a session. Practice
     * PCs are shared, so a leftover provider session must not let the next person in. A clinic that
     * wants single sign-on across its apps can switch this off.
     */
    val forceLogin: Boolean = true,
    /**
     * The provider's sign-out address, for providers whose sign-out does not follow the OpenID
     * Connect standard, such as Cognito. {postLogoutUri} is replaced with Aurae's own address.
     * Empty means the standard sign-out from the provider's discovery document.
     */
    val logoutUrl: String = "",
)
