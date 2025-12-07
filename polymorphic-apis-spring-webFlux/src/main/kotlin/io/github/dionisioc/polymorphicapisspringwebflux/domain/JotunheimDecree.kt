package io.github.dionisioc.polymorphicapisspringwebflux.domain

import jakarta.validation.constraints.Min

data class JotunheimDecree(
    override val commanderId: String,
    val eternalWinterEnabled: Boolean,

    @field:Min(value = 0, message = "There is no such thing as negative giants.")
    val frostGiantCount: Int
) : RealmDecree
