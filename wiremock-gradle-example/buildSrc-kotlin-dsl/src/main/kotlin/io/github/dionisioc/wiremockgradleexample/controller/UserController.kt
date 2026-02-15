package io.github.dionisioc.wiremockgradleexample.controller

import io.github.dionisioc.wiremockgradleexample.client.ExternalUserClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class UserController(
    private val client: ExternalUserClient,
    @Value("\${external.api.base-url}") private val externalUrl: String
) {
    @GetMapping("/api/users/{id}")
    suspend fun getUser(@PathVariable id: String): UserDto {
        val user = client.fetchUser(externalUrl, id)

        return UserDto(user.id, user.username.uppercase())
    }
}

data class UserDto(val id: String, val displayName: String)