package io.github.dionisioc.spinalcord

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SpinalcordApplication

fun main(args: Array<String>) {
	runApplication<SpinalcordApplication>(*args)
}
