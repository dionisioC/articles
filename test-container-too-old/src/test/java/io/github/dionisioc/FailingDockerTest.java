package io.github.dionisioc;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

class FailingDockerTest {

    @Test
    void testShouldFailOnDocker29() {
        try (GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse("alpine:3"))) {

            System.out.println("Attempting to start container...");

            container.start();

            System.out.println("Success! Container is running (Unexpected without fix).");
        }
    }
}