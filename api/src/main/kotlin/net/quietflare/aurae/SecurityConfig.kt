package net.quietflare.aurae

import net.quietflare.aurae.security.SecurityProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher

/**
 * Backend-for-frontend sign-in. Spring does the OpenID Connect login and keeps the tokens on the
 * server. The browser only holds an HttpOnly session cookie that page scripts cannot read.
 */
@Configuration
@EnableConfigurationProperties(SecurityProperties::class)
// Lets each endpoint declare its permission with @PreAuthorize("hasAuthority('report:upload')").
@EnableMethodSecurity
class SecurityConfig {

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        clients: ClientRegistrationRepository,
        properties: SecurityProperties,
    ): SecurityFilterChain {
        // The frontend reads the XSRF-TOKEN cookie and sends it back as a header on every change.
        http.csrf { it.spa() }

        http {
            authorizeHttpRequests {
                authorize("/api/**", authenticated)
                authorize("/actuator/health/**", permitAll)
                authorize("/actuator/**", denyAll)
                authorize(anyRequest, permitAll)
            }
            oauth2Login {
                authorizationEndpoint {
                    authorizationRequestResolver = signInRequests(clients, properties)
                }
            }
            exceptionHandling {
                // API calls get a plain 401 and the frontend decides to sign in. Only page loads redirect.
                defaultAuthenticationEntryPointFor(
                    HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    PathPatternRequestMatcher.withDefaults().matcher("/api/**"),
                )
            }
            logout {
                // Ends the provider's session too, not only Aurae's.
                logoutSuccessHandler = providerSignOut(clients, properties)
            }
        }
        return http.build()
    }

    private fun providerSignOut(clients: ClientRegistrationRepository, properties: SecurityProperties): LogoutSuccessHandler {
        if (properties.logoutUrl.isBlank()) {
            // Standard OpenID Connect sign-out, as Keycloak and Entra ID support it.
            return OidcClientInitiatedLogoutSuccessHandler(clients).apply {
                setPostLogoutRedirectUri("{baseUrl}/")
            }
        }
        // A provider with its own sign-out parameters, configured as a URL template.
        return LogoutSuccessHandler { request, response, _ ->
            val postLogoutUri = ServletUriComponentsBuilder.fromContextPath(request).path("/").toUriString()
            val encoded = URLEncoder.encode(postLogoutUri, StandardCharsets.UTF_8)
            response.sendRedirect(properties.logoutUrl.replace("{postLogoutUri}", encoded))
        }
    }

    // Builds the redirect to the provider. PKCE protects the code on its way back, and with
    // force-login on, prompt=login makes the provider ask for the password every time.
    private fun signInRequests(
        clients: ClientRegistrationRepository,
        properties: SecurityProperties,
    ): OAuth2AuthorizationRequestResolver {
        val resolver = DefaultOAuth2AuthorizationRequestResolver(clients, "/oauth2/authorization")
        var customizer = OAuth2AuthorizationRequestCustomizers.withPkce()
        if (properties.forceLogin) {
            customizer = customizer.andThen { request ->
                request.additionalParameters { it["prompt"] = "login" }
            }
        }
        resolver.setAuthorizationRequestCustomizer(customizer)
        return resolver
    }
}
