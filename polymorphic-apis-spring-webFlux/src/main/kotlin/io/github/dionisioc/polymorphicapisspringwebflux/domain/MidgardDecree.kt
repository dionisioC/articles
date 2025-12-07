package io.github.dionisioc.polymorphicapisspringwebflux.domain

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin

data class MidgardDecree(
    override val commanderId: String,

    @field:DecimalMin(value = "0.0", message = "Tax cannot be negative.")
    @field:DecimalMax(value = "1.0", message = "Tax cannot exceed 100%. The peasants will revolt!")
    val harvestTaxRate: Double,

    val protectVillages: Boolean
) : RealmDecree
