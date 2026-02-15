plugins {
    id("org.springframework.boot") version Versions.springBoot
    id("io.spring.dependency-management") version Versions.dependencyManagement
    kotlin("jvm") version Versions.kotlin
    kotlin("plugin.spring") version Versions.kotlin
}

group = "io.github.dionisioc"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(Libs.springWebflux)
    implementation(Libs.kotlinReflect)
    implementation(Libs.coroutinesReactor)
    implementation(Libs.jacksonKotlin)

    testImplementation(Libs.springTest)
    testImplementation(Libs.springWebfluxTest)
    testImplementation(Libs.testcontainers)

    testImplementation(Libs.wiremockStandalone)
    testImplementation(Libs.wiremockTcModule)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()

    systemProperty("TEST_WIREMOCK_VERSION", Versions.wiremock)
}