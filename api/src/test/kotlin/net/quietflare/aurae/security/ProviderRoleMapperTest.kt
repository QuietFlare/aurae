package net.quietflare.aurae.security

import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.env.StandardEnvironment
import org.springframework.core.io.ClassPathResource
import org.springframework.boot.context.properties.source.ConfigurationPropertySources
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

/** Checks the permission matrix exactly as application.yaml configures it. */
class ProviderRoleMapperTest {

    private val mapper = ProviderRoleMapper(propertiesFromApplicationYaml())

    private val clinical = setOf(
        Permission.REPORT_UPLOAD,
        Permission.REPORT_REVIEW,
        Permission.REPORT_SEND_TO_PVS,
        Permission.PATIENT_VALUES_READ,
        Permission.REPORT_ORIGINAL_READ,
    )

    @Test
    fun `mfa gets the clinical permissions`() {
        assertEquals(setOf("ROLE_MFA") + clinical, mappedFor("mfa"))
    }

    @Test
    fun `gp gets the same clinical permissions as mfa`() {
        assertEquals(setOf("ROLE_GP") + clinical, mappedFor("gp"))
    }

    @Test
    fun `admin manages users and reads the audit log but sees no medical data`() {
        assertEquals(setOf("ROLE_ADMIN", Permission.USERS_MANAGE, Permission.AUDIT_READ), mappedFor("admin"))
    }

    @Test
    fun `unknown provider roles grant nothing`() {
        assertEquals(emptySet(), mappedFor("receptionist"))
    }

    // Only the authorities the mapper adds, without the provider's own OIDC_USER entries.
    private fun mappedFor(vararg providerRoles: String): Set<String> {
        val idToken = OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(60), mapOf("sub" to "u1", "roles" to providerRoles.toList()))
        val original: Collection<GrantedAuthority> = listOf(OidcUserAuthority(idToken))
        return mapper.mapAuthorities(original).drop(original.size).mapNotNull { it.authority }.toSet()
    }

    private fun propertiesFromApplicationYaml(): SecurityProperties {
        val environment = StandardEnvironment()
        YamlPropertySourceLoader().load("application", ClassPathResource("application.yaml"))
            .forEach { environment.propertySources.addFirst(it) }
        return Binder(ConfigurationPropertySources.get(environment))
            .bind("aurae.security", SecurityProperties::class.java)
            .get()
    }
}
