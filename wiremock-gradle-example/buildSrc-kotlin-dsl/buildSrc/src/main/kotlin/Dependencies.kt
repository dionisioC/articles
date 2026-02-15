object Versions {
    const val kotlin = "2.2.21"
    const val springBoot = "4.0.2"
    const val dependencyManagement = "1.1.7"

    const val wiremock = "3.13.2"
    const val wiremockTc = "1.0-alpha-15"
}

object Libs {
    const val springWebflux = "org.springframework.boot:spring-boot-starter-webflux"
    const val springTest = "org.springframework.boot:spring-boot-starter-test"
    const val springWebfluxTest = "org.springframework.boot:spring-boot-starter-webflux-test"
    const val testcontainers = "org.testcontainers:testcontainers-junit-jupiter"

    const val kotlinReflect = "org.jetbrains.kotlin:kotlin-reflect"
    const val coroutinesReactor = "org.jetbrains.kotlinx:kotlinx-coroutines-reactor"
    const val jacksonKotlin = "com.fasterxml.jackson.module:jackson-module-kotlin"

    const val wiremockStandalone = "org.wiremock:wiremock-standalone:${Versions.wiremock}"
    const val wiremockTcModule = "org.wiremock.integrations.testcontainers:wiremock-testcontainers-module:${Versions.wiremockTc}"
}
