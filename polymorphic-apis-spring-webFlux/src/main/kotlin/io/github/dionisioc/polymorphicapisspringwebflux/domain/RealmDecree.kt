package io.github.dionisioc.polymorphicapisspringwebflux.domain

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import jakarta.validation.constraints.NotBlank

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "realm"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = MidgardDecree::class, name = "MIDGARD"),
    JsonSubTypes.Type(value = JotunheimDecree::class, name = "JOTUNHEIM")
)
sealed interface RealmDecree {
    @get:NotBlank(message = "The Commander ID cannot be empty. Reveal yourself!")
    val commanderId: String
}