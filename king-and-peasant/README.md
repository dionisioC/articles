# Handling Dynamic Authentication with the Strategy and Factory Patterns

In any long-lived software project, the only guarantee is change. An internal API that was once public suddenly needs to
be secured. A staging environment might allow anonymous access, while production requires rigorous **Defense in Depth**.

A high-security production environment often requires two locks:

1. **Transport Layer (mTLS):** Validates **Machine Identity**. Without a valid Certificate, the connection is dropped at
   the TCP handshake.
2. **Application Layer (Token):** Validates **User Permissions**. Even with a Certificate, you need a secret token to
   perform sensitive actions.

A naive design might hard-code these rules directly into the client, leading to a brittle mess of `if/else` statements
and complex SSL configuration blocks that are a nightmare to test and maintain.

A more resilient design anticipates this volatility. By using a combination of the **Strategy** and **Factory**
patterns, we can build a client that is decoupled, easy to test, and ready for any future requirement, while
remaining "closed for modification but open for extension."

## The Kingdom

Let's visualize our API as a Kingdom with three levels of access:

1. **The Barbarian (No Cert):** Has no identification. The drawbridge remains up. They cannot connect at all.
2. **The Citizen (Cert Only):** Has a valid passport (Certificate). They can enter the kingdom and talk to Peasants, but
   cannot enter the castle.
3. **The King (Cert + Token):** Has a passport *and* the Royal Password (Token). They can access everything.

### 1. The Server Configuration

First, we configure the server to enforce **mTLS**. If the client doesn't present a trusted certificate, the server
rejects the connection immediately.

```properties
spring.application.name=kingAndPeasant
server.port=8443
server.ssl.enabled=true
server.ssl.key-store=file:certs/server.p12
server.ssl.key-store-password=changeit
server.ssl.key-store-type=PKCS12
server.ssl.trust-store=file:certs/truststore.p12
server.ssl.trust-store-password=changeit
server.ssl.trust-store-type=PKCS12
server.ssl.client-auth=need
```

### 2. The Security Configuration

This is an example, don't take this as a production ready security configuration.

While `application.properties` configures the **Hardware Lock** (SSL Handshake), we need a **Software Lock** to
interpret that handshake and validate the secondary token.

To keep the Controller clean, we implement a **Filter**. This is a Royal Guard standing inside the castle hallway.

1. He checks if the person has already passed the drawbridge (mTLS/Certificate).
2. If they have, he checks if they are holding the Royal Password (Token).
3. If both are true, he upgrades their rank to **King**.

#### The Royal Token Filter

```kotlin
class RoyalTokenFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain
    ) {
        val currentAuth = SecurityContextHolder.getContext().authentication

        if (currentAuth != null && currentAuth.isAuthenticated) {
            val header = request.getHeader("Authorization")

            if (header == "Bearer MyRoyalSecret") {

                val royalAuth = UsernamePasswordAuthenticationToken(
                    currentAuth.principal,
                    currentAuth.credentials,
                    AuthorityUtils.createAuthorityList("ROLE_CITIZEN", "ROLE_KING")
                )

                SecurityContextHolder.getContext().authentication = royalAuth
            }
        }

        filterChain.doFilter(request, response)
    }
}
```

#### The Security Bean Wiring

Now we tell Spring Boot to place our `RoyalTokenFilter` **after** the standard X.509 check.

```kotlin
@Configuration
@EnableWebSecurity
class SecurityConfiguration {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.x509 { x509 ->
            x509.subjectPrincipalRegex("CN=(.*?)(?:,|$)")
        }.addFilterAfter(RoyalTokenFilter(), X509AuthenticationFilter::class.java).authorizeHttpRequests { auth ->
            auth.requestMatchers("/peasant").hasRole("CITIZEN")
            auth.requestMatchers("/king").hasRole("KING")
            auth.anyRequest().authenticated()
        }.csrf { it.disable() }

        return http.build()
    }

    @Bean
    fun userDetailsService(): UserDetailsService {
        return UserDetailsService { username ->
            User.withUsername(username).password("").roles("CITIZEN").build()
        }
    }
}
```

#### Why hardcode "CITIZEN"?

You might notice that `UserDetailsService` assigns `roles("CITIZEN")` to *everyone*. In a standard app, we would look up
roles in a database.

However, in this **Defense in Depth** architecture, we are decoupling **Identity** from **Permissions**:

1. **Identity (The Certificate):** The generic `CITIZEN` role simply means "This connection was successfully established
   with a valid, trusted certificate." The `UserDetailsService` confirms the *hardware* lock was passed.
2. **Permissions (The Token):** The `RoyalTokenFilter` handles the specific privileges. It decides who gets upgraded to
   `KING`.

This separation allows us to rotate certificates without worrying about application roles, and change application roles
without re-issuing certificates.

### 3. The Controller

The Controller handles purely business logic. It assumes that if the request reached the `talkToKing` method, the user
*must* be a King.

```kotlin
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
```

## 4. Verifying Security Rules

Before we generate real certificates, we can use Spring's `@WebMvcTest` to verify our security rules and our custom
filter logic. This allows us to simulate X.509 certificates without needing physical files.

We use `MockMvc` combined with Spring Security's testing tools to simulate different users.

```kotlin
@WebMvcTest(KingdomController::class)
@Import(SecurityConfiguration::class)
class KingdomSecurityTest {

    @Autowired
    private lateinit var mvc: MockMvc

    @Test
    fun `Barbarian (No Cert) is rejected at the gate`() {
        mvc.perform(get("/peasant"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `Citizen (Cert Only) can talk to Peasant`() {
        mvc.perform(
            get("/peasant")
                .with(mockX509("CN=Bob"))
        )
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("Greetings")))
    }

    @Test
    fun `Citizen (Cert Only) is Forbidden from the Castle`() {
        mvc.perform(
            get("/king")
                .with(mockX509("CN=Bob"))
        )
            .andExpect(status().isForbidden) // 403: Identity known, permissions denied
    }

    @Test
    fun `King (Cert + Token) is welcomed`() {
        mvc.perform(
            get("/king")
                .with(mockX509("CN=Arthur")) // 1. Identity
                .header("Authorization", "Bearer MyRoyalSecret") // 2. Permissions
        )
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("Your Majesty")))
    }

    private fun mockX509(dn: String): RequestPostProcessor {
        val cert = mock<X509Certificate>()
        val principal = X500Principal(dn)

        whenever(cert.subjectX500Principal).thenReturn(principal)
        whenever(cert.subjectDN).thenReturn(principal)

        return SecurityMockMvcRequestPostProcessors.x509(cert)
    }
}
```

## 5. Decoupling the "How": The Strategy Pattern

We need a Strategy interface that handles the two distinct lifecycles of a secure request: **Configuring the Client** (
SSL) and **Signing the Request** (Tokens).

The core problem with this security model requirement is that it touches two different layers of the networking stack:

1. **Transport Layer (mTLS):** Requires configuring the `SSLContext` of the HTTP Client *before* any connection is made.
2. **Application Layer (Token):** Requires injecting a Header *during* the request construction.

If we put all this logic inside the `ApiClient`, it violates the Single Responsibility Principle. We need a Strategy
interface that can handle both lifecycles.

### The Strategy Interface

We define a contract that allows a strategy to intervene at both the client creation stage and the request creation
stage.

```kotlin
interface AuthStrategy {
    /**
     * Configures the underlying connection (SSL, Timeouts).
     * Runs ONCE during initialization.
     */
    fun configureClient(builder: HttpClient.Builder): HttpClient.Builder

    /**
     * Applies headers to a specific request.
     * Runs EVERY time a request is sent.
     */
    fun applyToRequest(builder: HttpRequest.Builder): HttpRequest.Builder
}
```

### The Concrete Strategies

Next, we create the implementations.

#### 1. The Barbarian Strategy (No Auth)

Pass-through implementation. Used when testing against non-secured environments.

```kotlin
class NoAuthStrategy : AuthStrategy {
    override fun configureClient(builder: HttpClient.Builder) = builder
    override fun applyToRequest(builder: HttpRequest.Builder) = builder
}
```

#### 2. The Citizen Strategy (mTLS Only)

This strategy loads the KeyStore (Identity) and TrustStore (Validation) to establish the SSL connection. It uses
**TLSv1.3** for maximum security.

```kotlin
class CitizenStrategy(
    private val keyStorePath: Path,
    private val keyStorePassword: String,
    private val trustStorePath: Path,
    private val trustStorePassword: String
) : AuthStrategy {

    override fun configureClient(builder: HttpClient.Builder): HttpClient.Builder {
        val keyStore = KeyStore.getInstance("PKCS12")
        Files.newInputStream(keyStorePath).use {
            keyStore.load(it, keyStorePassword.toCharArray())
        }
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(keyStore, keyStorePassword.toCharArray())

        val trustStore = KeyStore.getInstance("PKCS12")
        Files.newInputStream(trustStorePath).use {
            trustStore.load(it, trustStorePassword.toCharArray())
        }
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(trustStore)

        val sslContext = SSLContext.getInstance("TLSv1.3")
        sslContext.init(kmf.keyManagers, tmf.trustManagers, SecureRandom())

        return builder.sslContext(sslContext)
    }

    override fun applyToRequest(builder: HttpRequest.Builder): HttpRequest.Builder {
        return builder
    }
}
```

#### 3. The King Strategy (mTLS + Token)

The King needs the exact same SSL setup as the Citizen, but *also* needs to inject the Authorization header.

```kotlin
class KingStrategy(
    private val keyStorePath: Path,
    private val keyStorePassword: String,
    private val trustStorePath: Path,
    private val trustStorePassword: String,
    private val apiToken: String
) : AuthStrategy {

    override fun configureClient(builder: HttpClient.Builder): HttpClient.Builder {
        val keyStore = KeyStore.getInstance("PKCS12")
        Files.newInputStream(keyStorePath).use {
            keyStore.load(it, keyStorePassword.toCharArray())
        }
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(keyStore, keyStorePassword.toCharArray())

        val trustStore = KeyStore.getInstance("PKCS12")
        Files.newInputStream(trustStorePath).use {
            trustStore.load(it, trustStorePassword.toCharArray())
        }
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(trustStore)

        val sslContext = SSLContext.getInstance("TLSv1.3")
        sslContext.init(kmf.keyManagers, tmf.trustManagers, SecureRandom())

        return builder.sslContext(sslContext)
    }

    override fun applyToRequest(builder: HttpRequest.Builder): HttpRequest.Builder {
        return builder.header("Authorization", "Bearer $apiToken")
    }
}
```

### 6. The ApiClient

The `ApiClient` logic is now completely decoupled from authentication details. It supports both endpoints (
`talkToPeasant` and `talkToKing`) and delegates the security specifics to the strategy.

```kotlin
class ApiClient(
    private val authStrategy: AuthStrategy
) {
    private val client: HttpClient = authStrategy
        .configureClient(HttpClient.newBuilder())
        .build()

    private val baseUrl = "https://localhost:8443"

    fun talkToPeasant(): HttpResponse<String> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/peasant"))
            .GET()

        val finalRequest = authStrategy.applyToRequest(request).build()

        return client.send(finalRequest, HttpResponse.BodyHandlers.ofString())
    }

    fun talkToKing(): HttpResponse<String> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/king"))
            .GET()

        val finalRequest = authStrategy.applyToRequest(request).build()

        return client.send(finalRequest, HttpResponse.BodyHandlers.ofString())
    }
}
```

## 7. Decoupling the "When": The Factory Pattern

We have solved how to handle complex auth, but who builds these complex objects? We don't want our main code dealing
with file paths and passwords.

We also want to make this testable. Instead of hardcoding System.getenv(), we allow the environment provider to be
injected. This lets us swap real environment variables for a simple Map during our tests.

```kotlin
object ApiClientFactory {
    fun createClient(env: (String) -> String? = System::getenv): ApiClient {
        val role = env("KINGDOM_ROLE") ?: "BARBARIAN"

        fun required(key: String): String =
            env(key) ?: throw IllegalStateException("Missing configuration: $key")

        return when (role) {
            "KING" -> {
                val certPath = required("MTLS_KEYSTORE_PATH")
                val certPass = required("MTLS_KEYSTORE_PASSWORD")
                val trustPath = required("MTLS_TRUSTSTORE_PATH")
                val trustPass = required("MTLS_TRUSTSTORE_PASSWORD")
                val token = required("API_TOKEN")

                ApiClient(
                    KingStrategy(
                        Path.of(certPath), certPass,
                        Path.of(trustPath), trustPass,
                        token
                    )
                )
            }

            "CITIZEN" -> {
                val certPath = required("MTLS_KEYSTORE_PATH")
                val certPass = required("MTLS_KEYSTORE_PASSWORD")
                val trustPath = required("MTLS_TRUSTSTORE_PATH")
                val trustPass = required("MTLS_TRUSTSTORE_PASSWORD")

                ApiClient(
                    CitizenStrategy(
                        Path.of(certPath), certPass,
                        Path.of(trustPath), trustPass
                    )
                )
            }

            else -> {
                ApiClient(NoAuthStrategy())
            }
        }
    }
}
```

## 8. Testing the Kingdom

To verify the entire stack (Client -\> mTLS -\> Filter -\> Controller), we generate certificates and run an Integration
Test.

### 1. Generate Certificates (`certs/generate-certs.sh`)

Run this bash script to create the Certificate Authority, Server Certs, and Client Certs.

```bash
#!/bin/bash
set -e

echo "--- Cleaning up old certificates ---"
rm -f ./*.crt ./*.key ./*.csr ./*.p12 ./*.srl

PASSWORD="changeit"

echo "--- 1. Generate CA (Root of Trust) ---"
openssl req -new -newkey rsa:2048 -days 365 -nodes -x509 -subj "/CN=KingdomRootCA" -keyout ca.key -out ca.crt

echo "--- 2. Generate Server Cert (localhost) ---"
openssl req -new -newkey rsa:2048 -days 365 -nodes -subj "/CN=localhost" -keyout server.key -out server.csr
openssl x509 -req -in server.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out server.crt -sha256
openssl pkcs12 -export -in server.crt -inkey server.key -out server.p12 -name server -passout pass:$PASSWORD

echo "--- 3. Generate Client Cert (The Passport) ---"
openssl req -new -newkey rsa:2048 -days 365 -nodes -subj "/CN=LoyalSubject" -keyout client.key -out client.csr
openssl x509 -req -in client.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out client.crt -sha256
openssl pkcs12 -export -in client.crt -inkey client.key -out client.p12 -name client -passout pass:$PASSWORD

echo "--- 4. Create Truststore ---"
keytool -import -file ca.crt -alias kingdomCa -keystore truststore.p12 -storepass $PASSWORD -noprompt

echo "--- Done! ---"
echo "Generated: server.p12, client.p12, truststore.p12"
```

### 2. The Integration Test

This test proves that our Client Factory correctly talks to our Secure Server using three roles.

```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class KingdomIntegrationTest {

    private val certDir = Path.of("certs").toAbsolutePath()
    private val keystorePath = certDir.resolve("client.p12").toString()
    private val truststorePath = certDir.resolve("truststore.p12").toString()
    private val password = "changeit"

    @Test
    fun `Factory creates Barbarian Client by default`() {
        val emptyEnv = mapOf<String, String>()

        val client = ApiClientFactory.createClient { key -> emptyEnv[key] }

        assertThrows(Exception::class.java) {
            client.talkToPeasant()
        }
    }

    @Test
    fun `Factory creates Citizen Client when configured`() {
        val citizenEnv = mapOf(
            "KINGDOM_ROLE" to "CITIZEN",
            "MTLS_KEYSTORE_PATH" to keystorePath,
            "MTLS_KEYSTORE_PASSWORD" to password,
            "MTLS_TRUSTSTORE_PATH" to truststorePath,
            "MTLS_TRUSTSTORE_PASSWORD" to password
        )

        val client = ApiClientFactory.createClient { key -> citizenEnv[key] }

        assertEquals(200, client.talkToPeasant().statusCode())
        assertEquals(403, client.talkToKing().statusCode())
    }

    @Test
    fun `Factory creates King Client when configured`() {
        val kingEnv = mapOf(
            "KINGDOM_ROLE" to "KING",
            "MTLS_KEYSTORE_PATH" to keystorePath,
            "MTLS_KEYSTORE_PASSWORD" to password,
            "MTLS_TRUSTSTORE_PATH" to truststorePath,
            "MTLS_TRUSTSTORE_PASSWORD" to password,

            "API_TOKEN" to "MyRoyalSecret"
        )

        val client = ApiClientFactory.createClient { key -> kingEnv[key] }

        assertEquals(200, client.talkToKing().statusCode())
    }
}
```