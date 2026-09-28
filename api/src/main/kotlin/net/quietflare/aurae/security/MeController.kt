package net.quietflare.aurae.security

import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

data class Me(val username: String, val name: String?, val roles: List<String>)

/** Tells the frontend who is signed in. A 401 here means nobody is. */
@RestController
class MeController {

    // Roles come from the Authentication, where ProviderRoleMapper put them at sign-in.
    // The OidcUser inside it still carries only the provider's original authorities.
    @GetMapping("/api/me")
    fun me(authentication: Authentication): Me {
        val user = authentication.principal as OidcUser
        return Me(
            username = user.preferredUsername ?: user.name,
            name = user.fullName,
            roles = authentication.authorities.map { it.authority.orEmpty() }
                .filter { it.startsWith("ROLE_") }
                .map { it.removePrefix("ROLE_") },
        )
    }
}
