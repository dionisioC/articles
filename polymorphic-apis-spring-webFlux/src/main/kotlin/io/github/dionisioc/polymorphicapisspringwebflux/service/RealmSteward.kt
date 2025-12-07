package io.github.dionisioc.polymorphicapisspringwebflux.service

import io.github.dionisioc.polymorphicapisspringwebflux.domain.RealmDecree
import reactor.core.publisher.Mono

interface RealmSteward<T : RealmDecree> {
    fun rulesOver(clazz: Class<out RealmDecree>): Boolean
    fun executeWill(decree: T): Mono<Unit>
}