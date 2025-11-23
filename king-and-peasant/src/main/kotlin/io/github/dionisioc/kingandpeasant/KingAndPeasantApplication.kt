package io.github.dionisioc.kingandpeasant

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KingAndPeasantApplication

fun main(args: Array<String>) {
	runApplication<KingAndPeasantApplication>(*args)
}
