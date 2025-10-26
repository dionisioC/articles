package io.github.dionisioc.spinalcord.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class RestClientConfiguration(@param:Value("\${ass.service.url}") val assServiceUrl: String) {
    @Bean
    fun assRestClient(builder: RestClient.Builder): RestClient {
        return builder.baseUrl(assServiceUrl)
            .build()
    }

}