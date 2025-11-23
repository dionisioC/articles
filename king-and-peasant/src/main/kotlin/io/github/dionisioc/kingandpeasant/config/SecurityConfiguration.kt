package io.github.dionisioc.kingandpeasant.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.preauth.x509.X509AuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfiguration {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.x509 { x509 ->
            x509.subjectPrincipalRegex("CN=(.*?)(?:,|$)")
        }.addFilterAfter(RoyalTokenFilter(), X509AuthenticationFilter::class.java).authorizeHttpRequests { auth ->
            auth.requestMatchers("/peasant").hasRole("CITIZEN")
            auth.requestMatchers("/king").hasRole("KING")
            auth.anyRequest().authenticated()
        }.csrf { it.disable() }

        return http.build()
    }

    @Bean
    fun userDetailsService(): UserDetailsService {
        return UserDetailsService { username ->
            User.withUsername(username).password("").roles("CITIZEN").build()
        }
    }
}