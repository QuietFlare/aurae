package net.quietflare.aurae

import org.junit.jupiter.api.Tag
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.test.context.TestPropertySource
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

/**
 * Marks a test that starts the whole application against a real Postgres in a container.
 * ./gradlew test skips these. ./gradlew integrationTest runs them and needs Docker.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Tag("integration")
@SpringBootTest
@Import(PostgresFixture::class, FixedProvider::class)
@TestPropertySource(properties = ["aurae.oidc.client-id=test", "aurae.oidc.client-secret=test"])
annotation class IntegrationTest

/** The same Postgres image as development and deployment. Spring points the datasource at it. */
@TestConfiguration(proxyBeanMethods = false)
class PostgresFixture {
    @Bean
    @ServiceConnection
    fun postgres() = PostgreSQLContainer(
        DockerImageName.parse("pgvector/pgvector:pg17").asCompatibleSubstituteFor("postgres"),
    )
}

/** An identity provider with fixed addresses, so nothing is fetched over the network. */
@TestConfiguration(proxyBeanMethods = false)
class FixedProvider {
    @Bean
    fun clients(): ClientRegistrationRepository = InMemoryClientRegistrationRepository(
        ClientRegistration.withRegistrationId("oidc")
            .clientId("test")
            .clientSecret("test")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .scope("openid")
            .authorizationUri("https://provider.invalid/auth")
            .tokenUri("https://provider.invalid/token")
            .jwkSetUri("https://provider.invalid/keys")
            .userNameAttributeName("sub")
            .build(),
    )
}
