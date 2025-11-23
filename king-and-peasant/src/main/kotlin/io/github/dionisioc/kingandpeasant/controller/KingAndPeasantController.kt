package io.github.dionisioc.kingandpeasant.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class KingdomController {

    @GetMapping("/peasant")
    fun talkToPeasant(): Map<String, String> {
        return mapOf("message" to "Greetings, Fellow Citizen!")
    }

    @GetMapping("/king")
    fun talkToKing(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf("message" to "Welcome, Your Majesty."))
    }
}