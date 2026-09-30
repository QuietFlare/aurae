package net.quietflare.aurae.security

import net.quietflare.aurae.FixedProvider
import net.quietflare.aurae.SecurityConfig
import org.hamcrest.Matchers.containsString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.test.Test

/** Stands in for a real endpoint until one exists, to prove that a permission check is enforced. */
@RestController
class UploadProbeController {
    @GetMapping("/api/upload-probe")
    @PreAuthorize("hasAuthority('${Permission.REPORT_UPLOAD}')")
    fun probe() = "ok"
}

/** Checks the rules of the security filter chain without a running identity provider. */
@WebMvcTest(MeController::class, UploadProbeController::class)
@Import(SecurityConfig::class, ProviderRoleMapper::class, UploadProbeController::class, FixedProvider::class)
@TestPropertySource(properties = ["aurae.oidc.client-id=test", "aurae.oidc.client-secret=test"])
class SecurityRulesTest {

    @Autowired
    lateinit var mvc: MockMvc

    private val mfa = oidcLogin().authorities(SimpleGrantedAuthority("ROLE_MFA"), SimpleGrantedAuthority(Permission.REPORT_UPLOAD))
    private val admin = oidcLogin().authorities(SimpleGrantedAuthority("ROLE_ADMIN"), SimpleGrantedAuthority(Permission.USERS_MANAGE))

    @Test
    fun `api calls without a session get 401 and no redirect`() {
        mvc.get("/api/me").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `me returns the roles and permissions of the signed-in user`() {
        mvc.get("/api/me") { with(mfa) }.andExpect {
            status { isOk() }
            jsonPath("$.roles[0]") { value("MFA") }
            jsonPath("$.permissions[0]") { value(Permission.REPORT_UPLOAD) }
        }
    }

    @Test
    fun `an endpoint lets in a user who holds its permission`() {
        mvc.get("/api/upload-probe") { with(mfa) }.andExpect { status { isOk() } }
    }

    @Test
    fun `an endpoint refuses a signed-in user without its permission`() {
        mvc.get("/api/upload-probe") { with(admin) }.andExpect { status { isForbidden() } }
    }

    @Test
    fun `actuator endpoints other than health are closed even to signed-in users`() {
        mvc.get("/actuator/env") { with(admin) }.andExpect { status { isForbidden() } }
    }

    @Test
    fun `a change without the csrf token is refused`() {
        mvc.post("/logout") { with(mfa) }.andExpect { status { isForbidden() } }
    }

    @Test
    fun `a change with the csrf token goes through`() {
        mvc.post("/logout") { with(mfa); with(csrf()) }.andExpect { status { is3xxRedirection() } }
    }

    @Test
    fun `sign-in sends the user to the provider with pkce and a forced password prompt`() {
        mvc.get("/oauth2/authorization/oidc").andExpect {
            status { is3xxRedirection() }
            header { string("Location", containsString("https://provider.invalid/auth")) }
            header { string("Location", containsString("code_challenge_method=S256")) }
            header { string("Location", containsString("prompt=login")) }
        }
    }
}
