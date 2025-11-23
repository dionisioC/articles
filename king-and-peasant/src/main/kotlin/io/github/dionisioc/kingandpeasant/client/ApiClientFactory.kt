package io.github.dionisioc.kingandpeasant.client

import io.github.dionisioc.kingandpeasant.strategy.CitizenStrategy
import io.github.dionisioc.kingandpeasant.strategy.KingStrategy
import io.github.dionisioc.kingandpeasant.strategy.NoAuthStrategy
import java.nio.file.Path

object ApiClientFactory {
    fun createClient(env: (String) -> String? = System::getenv): ApiClient {
        val role = env("KINGDOM_ROLE") ?: "BARBARIAN"

        fun required(key: String): String =
            env(key) ?: throw IllegalStateException("Missing configuration: $key")

        return when (role) {
            "KING" -> {
                val certPath = required("MTLS_KEYSTORE_PATH")
                val certPass = required("MTLS_KEYSTORE_PASSWORD")
                val trustPath = required("MTLS_TRUSTSTORE_PATH")
                val trustPass = required("MTLS_TRUSTSTORE_PASSWORD")
                val token = required("API_TOKEN")

                ApiClient(
                    KingStrategy(
                        Path.of(certPath), certPass,
                        Path.of(trustPath), trustPass,
                        token
                    )
                )
            }

            "CITIZEN" -> {
                val certPath = required("MTLS_KEYSTORE_PATH")
                val certPass = required("MTLS_KEYSTORE_PASSWORD")
                val trustPath = required("MTLS_TRUSTSTORE_PATH")
                val trustPass = required("MTLS_TRUSTSTORE_PASSWORD")

                ApiClient(
                    CitizenStrategy(
                        Path.of(certPath), certPass,
                        Path.of(trustPath), trustPass
                    )
                )
            }

            else -> {
                ApiClient(NoAuthStrategy())
            }
        }
    }
}