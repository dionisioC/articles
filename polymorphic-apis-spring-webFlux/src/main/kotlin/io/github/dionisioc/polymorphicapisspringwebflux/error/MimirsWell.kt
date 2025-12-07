package io.github.dionisioc.polymorphicapisspringwebflux.error

import org.springframework.core.codec.DecodingException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.server.ServerWebInputException
import tools.jackson.databind.exc.InvalidTypeIdException
import java.time.Instant

@RestControllerAdvice
class MimirsWell {

    @ExceptionHandler(WebExchangeBindException::class)
    fun handleBrokenLaws(ex: WebExchangeBindException): ResponseEntity<OracleProphecy> {
        val violations = ex.bindingResult.fieldErrors.map { "${it.field}: ${it.defaultMessage}" }

        return buildResponse(
            "The Guards Block Your Path", "Your decree violates the laws: $violations", HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler(ServerWebInputException::class)
    fun handleBadScrolls(ex: ServerWebInputException): ResponseEntity<OracleProphecy> {
        val cause = ex.cause

        val (title, wisdom) = if (cause is DecodingException && cause.cause is InvalidTypeIdException) {
            "The Bifrost Rejects This Realm" to "Unknown 'realm' type provided. Only MIDGARD and JOTUNHEIM are sanctioned."
        } else {
            "The Scroll is Torn" to "JSON parsing failed. Check your syntax."
        }

        return buildResponse(title, wisdom, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(Exception::class)
    fun handleRagnarok(ex: Exception): ResponseEntity<OracleProphecy> {
        return buildResponse(
            title = "Ragnarok is Upon Us",
            wisdom = "An internal error occurred: ${ex.message}",
            status = HttpStatus.INTERNAL_SERVER_ERROR
        )
    }

    private fun buildResponse(title: String, wisdom: String, status: HttpStatus): ResponseEntity<OracleProphecy> {
        val prophecy = OracleProphecy(title, wisdom, Instant.now().toString())
        return ResponseEntity.status(status).body(prophecy)
    }
}