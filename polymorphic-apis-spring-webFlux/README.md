# The Bifröst Pattern: Mastering Polymorphic APIs in Spring WebFlux

In the world of microservices, we often face a specific dilemma: **The Polymorphic Payload.** You need a single
endpoint (e.g., `PUT /api/config`) to update configurations, but the shape of that configuration changes depending on
*what* you are updating.

* **User Configs** have themes and notification settings.
* **System Configs** have maintenance modes and connection limits.

We will reimagine our architecture as the **High Seat of Odin** managing the Nine Realms.

## The Architecture of Asgard

Odin Allfather sits upon Hlidskjalf. He needs to issue decrees to govern the realms. However, the needs of **Midgard** (
the realm of humans) are different from the needs of **Jotunheim** (the realm of giants).

* **Midgard** needs configuration for `protectVillages` and `harvestTaxRate`.
* **Jotunheim** needs configuration for `eternalWinterEnabled` and `frostGiantCount`.

We will build **The Bifröst** a single API endpoint that accepts *any* decree, automatically understands which realm it
belongs to, checks it for madness (Validation), and routes it to the correct God (Service) for execution.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.0.0</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>

    <groupId>io.github.dionisioc</groupId>
    <artifactId>polymorphic-apis-spring-webFlux</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>polymorphic-apis-spring-webFlux</name>
    <description>polymorphic-apis-spring-webFlux</description>

    <properties>
        <java.version>21</java.version>
        <kotlin.version>2.2.21</kotlin.version>
        <mockito-kotlin.version>6.1.0</mockito-kotlin.version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>
        <dependency>
            <groupId>io.projectreactor.kotlin</groupId>
            <artifactId>reactor-kotlin-extensions</artifactId>
        </dependency>
        <dependency>
            <groupId>org.jetbrains.kotlin</groupId>
            <artifactId>kotlin-reflect</artifactId>
        </dependency>
        <dependency>
            <groupId>org.jetbrains.kotlin</groupId>
            <artifactId>kotlin-stdlib</artifactId>
        </dependency>
        <dependency>
            <groupId>org.jetbrains.kotlinx</groupId>
            <artifactId>kotlinx-coroutines-reactor</artifactId>
        </dependency>
        <dependency>
            <groupId>tools.jackson.module</groupId>
            <artifactId>jackson-module-kotlin</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.jetbrains.kotlin</groupId>
            <artifactId>kotlin-test-junit5</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.jetbrains.kotlinx</groupId>
            <artifactId>kotlinx-coroutines-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito.kotlin</groupId>
            <artifactId>mockito-kotlin</artifactId>
            <version>${mockito-kotlin.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <sourceDirectory>${project.basedir}/src/main/kotlin</sourceDirectory>
        <testSourceDirectory>${project.basedir}/src/test/kotlin</testSourceDirectory>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
            <plugin>
                <groupId>org.jetbrains.kotlin</groupId>
                <artifactId>kotlin-maven-plugin</artifactId>
                <configuration>
                    <args>
                        <arg>-Xjsr305=strict</arg>
                        <arg>-Xannotation-default-target=param-property</arg>
                    </args>
                    <compilerPlugins>
                        <plugin>spring</plugin>
                    </compilerPlugins>
                </configuration>
                <dependencies>
                    <dependency>
                        <groupId>org.jetbrains.kotlin</groupId>
                        <artifactId>kotlin-maven-allopen</artifactId>
                        <version>${kotlin.version}</version>
                    </dependency>
                </dependencies>
            </plugin>
        </plugins>
    </build>

</project>
```

-----

## The Decrees (Polymorphic Models)

We use Jackson’s **Polymorphic Deserialization**. We define a `sealed interface` representing a generic Decree. We use
`@JsonTypeInfo` to tell Jackson to look for a specific "discriminator" field (the Sigil) to decide which subclass to
create.

We also deploy **The Guards** (Jakarta Validation) to ensure no one tries to set negative tax rates or create negative
Frost Giants.

```kotlin
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import jakarta.validation.constraints.NotBlank

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "realm"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = MidgardDecree::class, name = "MIDGARD"),
    JsonSubTypes.Type(value = JotunheimDecree::class, name = "JOTUNHEIM")
)
sealed interface RealmDecree {
    @get:NotBlank(message = "The Commander ID cannot be empty. Reveal yourself!")
    val commanderId: String
}
```

```kotlin
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin

data class MidgardDecree(
    override val commanderId: String,

    @field:DecimalMin(value = "0.0", message = "Tax cannot be negative.")
    @field:DecimalMax(value = "1.0", message = "Tax cannot exceed 100%. The peasants will revolt!")
    val harvestTaxRate: Double,

    val protectVillages: Boolean
) : RealmDecree
```

```kotlin
import jakarta.validation.constraints.Min

data class JotunheimDecree(
    override val commanderId: String,
    val eternalWinterEnabled: Boolean,

    @field:Min(value = 0, message = "There is no such thing as negative giants.")
    val frostGiantCount: Int
) : RealmDecree
```

-----

## The Stewards (Strategy Pattern)

Odin does not micromanage. He delegates.
We use the **Strategy Pattern**. Each realm has a specific Steward (Service) responsible for executing the decree. Thor
handles Giants; Freya handles Humans.

```kotlin
import io.github.dionisioc.polymorphicapisspringwebflux.domain.RealmDecree
import reactor.core.publisher.Mono

interface RealmSteward<T : RealmDecree> {
    fun rulesOver(clazz: Class<out RealmDecree>): Boolean
    fun executeWill(decree: T): Mono<Unit>
}
```

```kotlin
import io.github.dionisioc.polymorphicapisspringwebflux.domain.MidgardDecree
import io.github.dionisioc.polymorphicapisspringwebflux.domain.RealmDecree
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class FreyaSteward : RealmSteward<MidgardDecree> {

    override fun rulesOver(clazz: Class<out RealmDecree>) = clazz == MidgardDecree::class.java

    override fun executeWill(decree: MidgardDecree): Mono<Unit> {
        return Mono.fromRunnable {
            require(!(decree.protectVillages && decree.harvestTaxRate == 0.0)) { "The Valkyries do not fly for free! Tax cannot be 0 if protection is requested." }

            println("🌱 FREYA: Logic executed for ${decree.commanderId}")
        }
    }
}
```

```kotlin
import io.github.dionisioc.polymorphicapisspringwebflux.domain.JotunheimDecree
import io.github.dionisioc.polymorphicapisspringwebflux.domain.RealmDecree
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class ThorSteward : RealmSteward<JotunheimDecree> {

    override fun rulesOver(clazz: Class<out RealmDecree>) = clazz == JotunheimDecree::class.java

    override fun executeWill(decree: JotunheimDecree): Mono<Unit> {
        return Mono.fromRunnable {
            check(decree.frostGiantCount <= 10_000) { "Too many giants! We need the entire Aesir army, not just Thor." }
            println("⚡ THOR: Logic executed for ${decree.commanderId}")
        }
    }
}
```

-----

## The Bifröst Gate (The Controller)

Heimdall (The Controller) stands ready. He accepts the `RealmDecree` and routes it.

Critically, we add the **`@Valid`** annotation here. This orders the Guards to inspect the scroll before Heimdall even
looks at it.

```kotlin
import io.github.dionisioc.polymorphicapisspringwebflux.domain.RealmDecree
import io.github.dionisioc.polymorphicapisspringwebflux.service.RealmSteward
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/bifrost")
class HeimdallGate(
    private val stewards: List<RealmSteward<out RealmDecree>>
) {

    @PutMapping("/decree")
    @Suppress("UNCHECKED_CAST")
    fun receiveDecree(@Valid @RequestBody decree: RealmDecree): Mono<Unit> {
        val steward =
            stewards.find { it.rulesOver(decree::class.java) } as? RealmSteward<RealmDecree> ?: return Mono.error(
                IllegalArgumentException("❌ No God found to rule over this realm!")
            )

        return steward.executeWill(decree)
    }
}
```

-----

## Mimir's Well (Global Error Handling)

When chaos strikes (Exceptions), we do not want to show the user a terrifying stack trace. We want to intercept that
chaos and turn it into a clear, wise prophecy (a clean JSON error response).

We use **`@RestControllerAdvice`**. This is Mimir sitting at the root of Yggdrasil, turning errors into wisdom.

```kotlin
data class OracleProphecy(
    val title: String, val wisdom: String, val timestamp: String
)
```

```kotlin
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
```

-----

## The Testing Grounds

Finally, we enter the **Hall of Illusions**. We simulate the decrees of Odin and the trickery of Loki to ensure the
Bifröst stands strong.

We use **`@WebFluxTest`**. This allows us to test the Controller, the Validation, and the Error Handling without
spinning up the entire application.

```kotlin
import io.github.dionisioc.polymorphicapisspringwebflux.domain.JotunheimDecree
import io.github.dionisioc.polymorphicapisspringwebflux.domain.MidgardDecree
import io.github.dionisioc.polymorphicapisspringwebflux.service.FreyaSteward
import io.github.dionisioc.polymorphicapisspringwebflux.service.ThorSteward
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono

@WebFluxTest(HeimdallGate::class)
class HeimdallGateTests {

    @Autowired
    lateinit var webClient: WebTestClient

    @MockitoBean
    lateinit var freyaSteward: FreyaSteward

    @MockitoBean
    lateinit var thorSteward: ThorSteward

    @BeforeEach
    fun summonGods() {
        whenever(freyaSteward.rulesOver(MidgardDecree::class.java)).thenReturn(true)
        whenever(freyaSteward.executeWill(any())).thenReturn(Mono.empty())

        whenever(thorSteward.rulesOver(JotunheimDecree::class.java)).thenReturn(true)
        whenever(thorSteward.executeWill(any())).thenReturn(Mono.empty())
    }

    @Test
    fun `Routes MIDGARD decree to Freya`() {
        val scroll = """
            {
                "realm":"MIDGARD",
                "commanderId":"Odin",
                "harvestTaxRate":0.10,
                "protectVillages":true
            }
        """.trimIndent()

        webClient.put().uri("/api/bifrost/decree").contentType(MediaType.APPLICATION_JSON).bodyValue(scroll).exchange()
            .expectStatus().isOk

        verify(freyaSteward).executeWill(any<MidgardDecree>())
        verify(thorSteward, never()).executeWill(any())

    }

    @Test
    fun `Routes JOTUNHEIM decree to Thor`() {
        val giantScroll = """
            {
                "realm": "JOTUNHEIM",
                "commanderId": "Odin",
                "eternalWinterEnabled": true,
                "frostGiantCount": 5000
            }
        """.trimIndent()

        webClient.put().uri("/api/bifrost/decree").contentType(MediaType.APPLICATION_JSON).bodyValue(giantScroll)
            .exchange().expectStatus().isOk

        verify(thorSteward).executeWill(any<JotunheimDecree>())
        verify(freyaSteward, never()).executeWill(any())
    }

    @Test
    fun `Loki's Trick - Sending a decree for a Realm that does not exist`() {
        val trickScroll = """
            { 
                "realm": "NIFLHEIM", 
                "commanderId": "Loki" 
            }
        """.trimIndent()

        webClient.put().uri("/api/bifrost/decree").contentType(MediaType.APPLICATION_JSON).bodyValue(trickScroll)
            .exchange().expectStatus().isBadRequest.expectBody().jsonPath("$.title")
            .isEqualTo("The Bifrost Rejects This Realm")
    }

    @Test
    fun `The Guards reject a decree with negative Frost Giants`() {
        val madScroll = """
            { 
                "realm": "JOTUNHEIM", 
                "commanderId": "Mad-Loki", 
                "frostGiantCount": -50 
            }
        """.trimIndent()

        webClient.put().uri("/api/bifrost/decree").contentType(MediaType.APPLICATION_JSON).bodyValue(madScroll)
            .exchange().expectStatus().isBadRequest.expectBody().jsonPath("$.wisdom").value<String> {
                it.contains("frostGiantCount: There is no such thing as negative giants")
            }
    }

    @Test
    fun `The Bifrost handles errors when Thor refuses the command`() {
        whenever(thorSteward.executeWill(any())).thenReturn(Mono.error(IllegalStateException("We cannot fight this many giants!")))

        val scroll = """
            { 
                "realm": "JOTUNHEIM", 
                "commanderId": "Odin", 
                "eternalWinterEnabled": true, 
                "frostGiantCount": 900 
            }
        """.trimIndent()

        webClient.put().uri("/api/bifrost/decree").contentType(MediaType.APPLICATION_JSON).bodyValue(scroll).exchange()
            .expectStatus().is5xxServerError.expectBody().jsonPath("$.title").isEqualTo("Ragnarok is Upon Us")
    }
}
```

```kotlin
import io.github.dionisioc.polymorphicapisspringwebflux.domain.JotunheimDecree
import io.github.dionisioc.polymorphicapisspringwebflux.domain.MidgardDecree
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import reactor.test.StepVerifier

class FreyaStewardTests {

    private val freya = FreyaSteward()

    @Test
    fun `Freya claims dominion over Midgard decrees`() {
        assertTrue(freya.rulesOver(MidgardDecree::class.java))
    }

    @Test
    fun `Freya rejects dominion over Jotunheim decrees`() {
        assertFalse(freya.rulesOver(JotunheimDecree::class.java))
    }

    @Test
    fun `Freya accepts a standard decree`() {
        val validDecree = MidgardDecree(
            commanderId = "Odin",
            harvestTaxRate = 0.1,
            protectVillages = true
        )

        StepVerifier.create(freya.executeWill(validDecree))
            .expectComplete()
            .verify()
    }

    @Test
    fun `Freya refuses to protect villages if Tax is 0`() {
        val cheapDecree = MidgardDecree(
            commanderId = "Cheap-Odin",
            harvestTaxRate = 0.0,
            protectVillages = true
        )

        StepVerifier.create(freya.executeWill(cheapDecree))
            .expectErrorMatches { throwable ->
                throwable is IllegalArgumentException &&
                        throwable.message == "The Valkyries do not fly for free! Tax cannot be 0 if protection is requested."
            }
            .verify()
    }
}
```


```kotlin
import io.github.dionisioc.polymorphicapisspringwebflux.domain.JotunheimDecree
import io.github.dionisioc.polymorphicapisspringwebflux.domain.MidgardDecree
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import reactor.test.StepVerifier

class ThorStewardTests {

    private val thor = ThorSteward()

    @Test
    fun `Thor claims dominion over Jotunheim`() {
        assertTrue(thor.rulesOver(JotunheimDecree::class.java))
    }

    @Test
    fun `Thor rejects dominion over Midgard decrees`() {
        assertFalse(thor.rulesOver(MidgardDecree::class.java))
    }

    @Test
    fun `Thor happily fights a reasonable number of giants`() {
        val battlePlans = JotunheimDecree(
            commanderId = "Odin",
            eternalWinterEnabled = true,
            frostGiantCount = 500
        )

        StepVerifier.create(thor.executeWill(battlePlans))
            .expectComplete()
            .verify()
    }

    @Test
    fun `Thor refuses to fight an army that is too large`() {
        val suicideMission = JotunheimDecree(
            commanderId = "Loki",
            eternalWinterEnabled = true,
            frostGiantCount = 50_000
        )

        StepVerifier.create(thor.executeWill(suicideMission))
            .expectErrorMatches { error ->
                error is IllegalStateException &&
                        error.message!!.contains("Too many giants")
            }
            .verify()
    }
}
```