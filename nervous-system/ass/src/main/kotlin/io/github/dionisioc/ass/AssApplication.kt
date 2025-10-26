package io.github.dionisioc.ass

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AssApplication

fun main(args: Array<String>) {
	runApplication<AssApplication>(*args)
}
