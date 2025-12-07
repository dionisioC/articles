package io.github.dionisioc.polymorphicapisspringwebflux.api

import io.github.dionisioc.polymorphicapisspringwebflux.domain.JotunheimDecree
import io.github.dionisioc.polymorphicapisspringwebflux.domain.MidgardDecree
import io.github.dionisioc.polymorphicapisspringwebflux.service.FreyaSteward
import io.github.dionisioc.polymorphicapisspringwebflux.service.ThorSteward
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono

@WebFluxTest(HeimdallGate::class)
class HeimdallGateTests {

    @Autowired
    lateinit var webClient: WebTestClient

    @MockitoBean
    lateinit var freyaSteward: FreyaSteward

    @MockitoBean
    lateinit var thorSteward: ThorSteward

    @BeforeEach
    fun summonGods() {
        whenever(freyaSteward.rulesOver(MidgardDecree::class.java)).thenReturn(true)
        whenever(freyaSteward.executeWill(any())).thenReturn(Mono.empty())

        whenever(thorSteward.rulesOver(JotunheimDecree::class.java)).thenReturn(true)
        whenever(thorSteward.executeWill(any())).thenReturn(Mono.empty())
    }

    @Test
    fun `Routes MIDGARD decree to Freya`() {
        val scroll = """
            {
                "realm":"MIDGARD",
                "commanderId":"Odin",
                "harvestTaxRate":0.10,
                "protectVillages":true
            }
        """.trimIndent()

        webClient.put().uri("/api/bifrost/decree").contentType(MediaType.APPLICATION_JSON).bodyValue(scroll).exchange()
            .expectStatus().isOk

        verify(freyaSteward).executeWill(any<MidgardDecree>())
        verify(thorSteward, never()).executeWill(any())

    }

    @Test
    fun `Routes JOTUNHEIM decree to Thor`() {
        val giantScroll = """
            {
                "realm": "JOTUNHEIM",
                "commanderId": "Odin",
                "eternalWinterEnabled": true,
                "frostGiantCount": 5000
            }
        """.trimIndent()

        webClient.put().uri("/api/bifrost/decree").contentType(MediaType.APPLICATION_JSON).bodyValue(giantScroll)
            .exchange().expectStatus().isOk

        verify(thorSteward).executeWill(any<JotunheimDecree>())
        verify(freyaSteward, never()).executeWill(any())
    }

    @Test
    fun `Loki's Trick - Sending a decree for a Realm that does not exist`() {
        val trickScroll = """
            { 
                "realm": "NIFLHEIM", 
                "commanderId": "Loki" 
            }
        """.trimIndent()

        webClient.put().uri("/api/bifrost/decree").contentType(MediaType.APPLICATION_JSON).bodyValue(trickScroll)
            .exchange().expectStatus().isBadRequest.expectBody().jsonPath("$.title")
            .isEqualTo("The Bifrost Rejects This Realm")
    }

    @Test
    fun `The Guards reject a decree with negative Frost Giants`() {
        val madScroll = """
            { 
                "realm": "JOTUNHEIM", 
                "commanderId": "Mad-Loki", 
                "frostGiantCount": -50 
            }
        """.trimIndent()

        webClient.put().uri("/api/bifrost/decree").contentType(MediaType.APPLICATION_JSON).bodyValue(madScroll)
            .exchange().expectStatus().isBadRequest.expectBody().jsonPath("$.wisdom").value<String> {
                it.contains("frostGiantCount: There is no such thing as negative giants")
            }
    }

    @Test
    fun `The Bifrost handles errors when Thor refuses the command`() {
        whenever(thorSteward.executeWill(any())).thenReturn(Mono.error(IllegalStateException("We cannot fight this many giants!")))

        val scroll = """
            { 
                "realm": "JOTUNHEIM", 
                "commanderId": "Odin", 
                "eternalWinterEnabled": true, 
                "frostGiantCount": 900 
            }
        """.trimIndent()

        webClient.put().uri("/api/bifrost/decree").contentType(MediaType.APPLICATION_JSON).bodyValue(scroll).exchange()
            .expectStatus().is5xxServerError.expectBody().jsonPath("$.title").isEqualTo("Ragnarok is Upon Us")
    }
}