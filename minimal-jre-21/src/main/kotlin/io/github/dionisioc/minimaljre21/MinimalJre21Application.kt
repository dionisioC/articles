package io.github.dionisioc.minimaljre21

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MinimalJre21Application

fun main(args: Array<String>) {
    runApplication<MinimalJre21Application>(*args)
}
