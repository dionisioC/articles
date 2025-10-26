package io.github.dionisioc.ass.controller

import io.micrometer.tracing.Tracer
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class AssController(
    private val tracer: Tracer
) {

    private val logger = LoggerFactory.getLogger(AssController::class.java)

    @GetMapping("/poo")
    fun poo(): AssResponse {
        logger.info("'ass' service received request... responding with 'poo'")

        val currentTraceId = tracer.currentSpan()?.context()?.traceId()

        if (currentTraceId == null) {
            logger.error("No active trace found!")
            throw IllegalStateException("Trace ID could not be found")
        }

        return AssResponse(
            id = currentTraceId,
            messageContent = "I poo"
        )
    }

}