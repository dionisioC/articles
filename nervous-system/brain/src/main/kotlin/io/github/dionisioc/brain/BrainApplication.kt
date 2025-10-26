package io.github.dionisioc.brain

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class BrainApplication

fun main(args: Array<String>) {
	runApplication<BrainApplication>(*args)
}
