plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
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
    implementation(libs.spring.webflux)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.coroutines.reactor)
    implementation(libs.jackson.kotlin)

    testImplementation(libs.spring.test)
    testImplementation(libs.spring.webflux.test)
    testImplementation(libs.testcontainers.junit)

    testImplementation(libs.wiremock.standalone)
    testImplementation(libs.wiremock.testcontainers)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("TEST_WIREMOCK_VERSION", libs.versions.wiremock.get())
}