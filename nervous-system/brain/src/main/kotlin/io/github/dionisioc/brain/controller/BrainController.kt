package io.github.dionisioc.brain.controller

import io.awspring.cloud.sqs.operations.SqsTemplate
import io.micrometer.tracing.Tracer
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class BrainController(
    private val sqsTemplate: SqsTemplate,
    private val tracer: Tracer
) {
    private val logger = LoggerFactory.getLogger(BrainController::class.java)
    private val queueName = "spinal-cord"

    @GetMapping("/poo")
    fun startProcess(): BrainResponse {
        logger.info("Received request in 'brain'. Sending SQS message...")

        val currentTraceId = tracer.currentSpan()?.context()?.traceId()

        if (currentTraceId == null) {
            logger.error("No active trace found!")
            throw IllegalStateException("Trace ID could not be found")
        }

        sqsTemplate.send(queueName, "I want to poo")

        logger.info("Message sent to SQS.")
        return BrainResponse(
            id = currentTraceId,
            messageContent = "I want to poo"
        )
    }
}