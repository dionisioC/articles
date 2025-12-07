package io.github.dionisioc.polymorphicapisspringwebflux.service

import io.github.dionisioc.polymorphicapisspringwebflux.domain.MidgardDecree
import io.github.dionisioc.polymorphicapisspringwebflux.domain.RealmDecree
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class FreyaSteward : RealmSteward<MidgardDecree> {

    override fun rulesOver(clazz: Class<out RealmDecree>) = clazz == MidgardDecree::class.java

    override fun executeWill(decree: MidgardDecree): Mono<Unit> {
        return Mono.fromRunnable {
            require(!(decree.protectVillages && decree.harvestTaxRate == 0.0)) { "The Valkyries do not fly for free! Tax cannot be 0 if protection is requested." }

            println("🌱 FREYA: Logic executed for ${decree.commanderId}")
        }
    }
}