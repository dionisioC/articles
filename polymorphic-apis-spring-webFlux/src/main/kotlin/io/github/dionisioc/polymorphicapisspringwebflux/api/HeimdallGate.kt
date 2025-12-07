package io.github.dionisioc.polymorphicapisspringwebflux.api

import io.github.dionisioc.polymorphicapisspringwebflux.domain.RealmDecree
import io.github.dionisioc.polymorphicapisspringwebflux.service.RealmSteward
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/bifrost")
class HeimdallGate(
    private val stewards: List<RealmSteward<out RealmDecree>>
) {

    @PutMapping("/decree")
    @Suppress("UNCHECKED_CAST")
    fun receiveDecree(@Valid @RequestBody decree: RealmDecree): Mono<Unit> {
        val steward =
            stewards.find { it.rulesOver(decree::class.java) } as? RealmSteward<RealmDecree> ?: return Mono.error(
                IllegalArgumentException("❌ No God found to rule over this realm!")
            )

        return steward.executeWill(decree)
    }
}