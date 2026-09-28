package net.quietflare.aurae

import net.quietflare.aurae.security.SecurityProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher

/**
 * Backend-for-frontend sign-in. Spring does the OpenID Connect login and keeps the tokens on the
 * server. The browser only holds an HttpOnly session cookie that page scripts cannot read.
 */
@Configuration
@EnableConfigurationProperties(SecurityProperties::class)
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity, clients: ClientRegistrationRepository): SecurityFilterChain {
        // The frontend reads the XSRF-TOKEN cookie and sends it back as a header on every change.
        http.csrf { it.spa() }

        http {
            authorizeHttpRequests {
                authorize("/api/**", authenticated)
                authorize("/actuator/health/**", permitAll)
                authorize("/actuator/**", denyAll)
                authorize(anyRequest, permitAll)
            }
            oauth2Login { }
            exceptionHandling {
                // API calls get a plain 401 and the frontend decides to sign in. Only page loads redirect.
                defaultAuthenticationEntryPointFor(
                    HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    PathPatternRequestMatcher.withDefaults().matcher("/api/**"),
                )
            }
            logout {
                // Ends the Keycloak session too, not only Aurae's.
                logoutSuccessHandler = OidcClientInitiatedLogoutSuccessHandler(clients).apply {
                    setPostLogoutRedirectUri("{baseUrl}/")
                }
            }
        }
        return http.build()
    }
}
