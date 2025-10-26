package io.github.dionisioc.spinalcord.listener

import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.messaging.Message
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

private const val queueName = "spinal-cord"

@Component
class SqsListener(private val assRestClient: RestClient) {
    private val logger = LoggerFactory.getLogger(this::class.java)


    @SqsListener(queueNames = [queueName])
    fun receiveMessage(message: Message<String>) {
        logger.info("'vertebral-spine' received message: ${message.payload}")

        logger.info("Calling 'ass' service...")

        val response = assRestClient.get()
            .uri("/poo")
            .retrieve()
            .body(String::class.java)

        logger.info("'ass' service responded with: '$response'")
    }

}