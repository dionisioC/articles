package io.github.dionisioc.kingandpeasant.strategy

import java.net.http.HttpClient
import java.net.http.HttpRequest

interface AuthStrategy {
    /**
     * Configures the underlying connection (SSL, Timeouts).
     * Runs ONCE during initialization.
     */
    fun configureClient(builder: HttpClient.Builder): HttpClient.Builder

    /**
     * Applies headers to a specific request.
     * Runs EVERY time a request is sent.
     */
    fun applyToRequest(builder: HttpRequest.Builder): HttpRequest.Builder
}