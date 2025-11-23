package io.github.dionisioc.kingandpeasant.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class RoyalTokenFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain
    ) {
        val currentAuth = SecurityContextHolder.getContext().authentication

        if (currentAuth != null && currentAuth.isAuthenticated) {
            val header = request.getHeader("Authorization")

            if (header == "Bearer MyRoyalSecret") {

                val royalAuth = UsernamePasswordAuthenticationToken(
                    currentAuth.principal,
                    currentAuth.credentials,
                    AuthorityUtils.createAuthorityList("ROLE_CITIZEN", "ROLE_KING")
                )

                SecurityContextHolder.getContext().authentication = royalAuth
            }
        }

        filterChain.doFilter(request, response)
    }
}