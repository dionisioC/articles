package io.github.dionisioc.kingandpeasant

import io.github.dionisioc.kingandpeasant.client.ApiClientFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import java.nio.file.Path

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class KingdomIntegrationTest {

    private val certDir = Path.of("certs").toAbsolutePath()
    private val keystorePath = certDir.resolve("client.p12").toString()
    private val truststorePath = certDir.resolve("truststore.p12").toString()
    private val password = "changeit"

    @Test
    fun `Factory creates Barbarian Client by default`() {
        val emptyEnv = mapOf<String, String>()

        val client = ApiClientFactory.createClient { key -> emptyEnv[key] }

        assertThrows(Exception::class.java) {
            client.talkToPeasant()
        }
    }

    @Test
    fun `Factory creates Citizen Client when configured`() {
        val citizenEnv = mapOf(
            "KINGDOM_ROLE" to "CITIZEN",
            "MTLS_KEYSTORE_PATH" to keystorePath,
            "MTLS_KEYSTORE_PASSWORD" to password,
            "MTLS_TRUSTSTORE_PATH" to truststorePath,
            "MTLS_TRUSTSTORE_PASSWORD" to password
        )

        val client = ApiClientFactory.createClient { key -> citizenEnv[key] }

        assertEquals(200, client.talkToPeasant().statusCode())
        assertEquals(403, client.talkToKing().statusCode())
    }

    @Test
    fun `Factory creates King Client when configured`() {
        val kingEnv = mapOf(
            "KINGDOM_ROLE" to "KING",
            "MTLS_KEYSTORE_PATH" to keystorePath,
            "MTLS_KEYSTORE_PASSWORD" to password,
            "MTLS_TRUSTSTORE_PATH" to truststorePath,
            "MTLS_TRUSTSTORE_PASSWORD" to password,

            "API_TOKEN" to "MyRoyalSecret"
        )

        val client = ApiClientFactory.createClient { key -> kingEnv[key] }

        assertEquals(200, client.talkToKing().statusCode())
    }
}