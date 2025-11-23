package io.github.dionisioc.kingandpeasant.strategy

import java.net.http.HttpClient
import java.net.http.HttpRequest

class NoAuthStrategy : AuthStrategy {
    override fun configureClient(builder: HttpClient.Builder) = builder
    override fun applyToRequest(builder: HttpRequest.Builder) = builder
}