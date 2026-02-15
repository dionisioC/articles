package io.github.dionisioc.wiremockgradleexample

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.reactive.function.client.WebClient

@SpringBootApplication
class WiremockGradleExampleApplication {

    @Bean
    fun webClientBuilder(): WebClient.Builder = WebClient.builder()

}

fun main(args: Array<String>) {
    runApplication<WiremockGradleExampleApplication>(*args)
}
