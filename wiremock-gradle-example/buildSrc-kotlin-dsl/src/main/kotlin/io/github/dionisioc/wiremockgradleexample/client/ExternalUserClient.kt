package io.github.dionisioc.wiremockgradleexample.client

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

@Component
class ExternalUserClient(builder: WebClient.Builder) {
    private val client = builder.build()

    suspend fun fetchUser(baseUrl: String, id: String): UserResponse {
        return client.get()
            .uri("$baseUrl/external/users/{id}", id)
            .retrieve()
            .awaitBody<UserResponse>()
    }
}
data class UserResponse(val id: String, val username: String, val role: String)