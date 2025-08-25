package io.github.dionisioc.mockito.warnings

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MockitoJvmAgentWarningsApplication

fun main(args: Array<String>) {
	runApplication<MockitoJvmAgentWarningsApplication>(*args)
}
