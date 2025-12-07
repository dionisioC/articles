package io.github.dionisioc.polymorphicapisspringwebflux.service

import io.github.dionisioc.polymorphicapisspringwebflux.domain.JotunheimDecree
import io.github.dionisioc.polymorphicapisspringwebflux.domain.RealmDecree
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class ThorSteward : RealmSteward<JotunheimDecree> {

    override fun rulesOver(clazz: Class<out RealmDecree>) = clazz == JotunheimDecree::class.java

    override fun executeWill(decree: JotunheimDecree): Mono<Unit> {
        return Mono.fromRunnable {
            check(decree.frostGiantCount <= 10_000) { "Too many giants! We need the entire Aesir army, not just Thor." }
            println("⚡ THOR: Logic executed for ${decree.commanderId}")
        }
    }
}