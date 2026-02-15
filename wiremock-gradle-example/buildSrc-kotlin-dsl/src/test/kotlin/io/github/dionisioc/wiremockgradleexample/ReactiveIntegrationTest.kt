package io.github.dionisioc.wiremockgradleexample

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.containers.BindMode
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.wiremock.integrations.testcontainers.WireMockContainer

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Testcontainers
class ReactiveIntegrationTest {

    @Autowired
    lateinit var webTestClient: WebTestClient

    companion object {
        private val WIREMOCK_VERSION = System.getProperty("TEST_WIREMOCK_VERSION")

        @Container
        val wiremock = WireMockContainer("wiremock/wiremock:$WIREMOCK_VERSION")
            .withClasspathResourceMapping(
                "wiremock",
                "/home/wiremock",
                BindMode.READ_ONLY
            )

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("external.api.base-url") { wiremock.baseUrl }
        }
    }

    @BeforeEach
    fun setup() {
        WireMock.configureFor(wiremock.host, wiremock.getMappedPort(8080))
        WireMock.resetAllRequests()
    }

    @Test
    fun `should fetch and transform user data non-blocking`() {
        webTestClient.get()
            .uri("/api/users/101")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.displayName").isEqualTo("JOHN_DOE")

        verify(getRequestedFor(urlEqualTo("/external/users/101")))
    }
}