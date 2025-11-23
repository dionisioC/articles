package io.github.dionisioc.kingandpeasant.client

import io.github.dionisioc.kingandpeasant.strategy.AuthStrategy
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class ApiClient(
    private val authStrategy: AuthStrategy
) {
    private val client: HttpClient = authStrategy
        .configureClient(HttpClient.newBuilder())
        .build()

    private val baseUrl = "https://localhost:8443"

    fun talkToPeasant(): HttpResponse<String> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/peasant"))
            .GET()

        val finalRequest = authStrategy.applyToRequest(request).build()

        return client.send(finalRequest, HttpResponse.BodyHandlers.ofString())
    }

    fun talkToKing(): HttpResponse<String> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/king"))
            .GET()

        val finalRequest = authStrategy.applyToRequest(request).build()

        return client.send(finalRequest, HttpResponse.BodyHandlers.ofString())
    }
}