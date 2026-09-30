package net.quietflare.aurae.security

import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

data class Me(
    val username: String,
    val name: String?,
    val roles: List<String>,
    val permissions: List<String>,
    /** For plain form posts such as sign-out. Spring expects form fields to carry this masked value. */
    val csrfToken: String,
)

/** Tells the frontend who is signed in and what they may do. A 401 here means nobody is. */
@RestController
class MeController {

    // Roles and permissions come from the Authentication, where ProviderRoleMapper put them at
    // sign-in. The OidcUser inside it still carries only the provider's original authorities.
    @GetMapping("/api/me")
    fun me(authentication: Authentication, csrf: CsrfToken): Me {
        val user = authentication.principal as OidcUser
        val authorities = authentication.authorities.map { it.authority.orEmpty() }
        return Me(
            username = user.preferredUsername ?: user.name,
            name = user.fullName,
            roles = authorities.filter { it.startsWith("ROLE_") }.map { it.removePrefix("ROLE_") },
            permissions = authorities.filter { ':' in it }.sorted(),
            csrfToken = csrf.token,
        )
    }
}
