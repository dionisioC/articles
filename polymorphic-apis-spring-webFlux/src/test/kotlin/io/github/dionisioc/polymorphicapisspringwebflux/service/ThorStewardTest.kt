package io.github.dionisioc.polymorphicapisspringwebflux.service

import io.github.dionisioc.polymorphicapisspringwebflux.domain.JotunheimDecree
import io.github.dionisioc.polymorphicapisspringwebflux.domain.MidgardDecree
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import reactor.test.StepVerifier

class ThorStewardTests {

    private val thor = ThorSteward()

    @Test
    fun `Thor claims dominion over Jotunheim`() {
        assertTrue(thor.rulesOver(JotunheimDecree::class.java))
    }

    @Test
    fun `Thor rejects dominion over Midgard decrees`() {
        assertFalse(thor.rulesOver(MidgardDecree::class.java))
    }

    @Test
    fun `Thor happily fights a reasonable number of giants`() {
        val battlePlans = JotunheimDecree(
            commanderId = "Odin",
            eternalWinterEnabled = true,
            frostGiantCount = 500
        )

        StepVerifier.create(thor.executeWill(battlePlans))
            .expectComplete()
            .verify()
    }

    @Test
    fun `Thor refuses to fight an army that is too large`() {
        val suicideMission = JotunheimDecree(
            commanderId = "Loki",
            eternalWinterEnabled = true,
            frostGiantCount = 50_000
        )

        StepVerifier.create(thor.executeWill(suicideMission))
            .expectErrorMatches { error ->
                error is IllegalStateException &&
                        error.message!!.contains("Too many giants")
            }
            .verify()
    }
}