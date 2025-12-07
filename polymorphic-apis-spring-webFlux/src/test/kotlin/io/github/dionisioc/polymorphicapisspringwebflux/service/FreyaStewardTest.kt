package io.github.dionisioc.polymorphicapisspringwebflux.service

import io.github.dionisioc.polymorphicapisspringwebflux.domain.JotunheimDecree
import io.github.dionisioc.polymorphicapisspringwebflux.domain.MidgardDecree
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import reactor.test.StepVerifier

class FreyaStewardTests {

    private val freya = FreyaSteward()

    @Test
    fun `Freya claims dominion over Midgard decrees`() {
        assertTrue(freya.rulesOver(MidgardDecree::class.java))
    }

    @Test
    fun `Freya rejects dominion over Jotunheim decrees`() {
        assertFalse(freya.rulesOver(JotunheimDecree::class.java))
    }

    @Test
    fun `Freya accepts a standard decree`() {
        val validDecree = MidgardDecree(
            commanderId = "Odin",
            harvestTaxRate = 0.1,
            protectVillages = true
        )

        StepVerifier.create(freya.executeWill(validDecree))
            .expectComplete()
            .verify()
    }

    @Test
    fun `Freya refuses to protect villages if Tax is 0`() {
        val cheapDecree = MidgardDecree(
            commanderId = "Cheap-Odin",
            harvestTaxRate = 0.0,
            protectVillages = true
        )

        StepVerifier.create(freya.executeWill(cheapDecree))
            .expectErrorMatches { throwable ->
                throwable is IllegalArgumentException &&
                        throwable.message == "The Valkyries do not fly for free! Tax cannot be 0 if protection is requested."
            }
            .verify()
    }
}