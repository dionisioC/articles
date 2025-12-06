# Fixing "Client Version Too Old" Errors with Testcontainers on Linux

If you have recently updated docker engine to version [29](https://docs.docker.com/engine/release-notes/29/), there is a
breaking change that can give you (at least it gave to me) some headaches: **The daemon now requires API version v1.44
or later (Docker v25.0+)**

If you are using **Testcontainers 1.21.3** (or older), your build pipeline may suddenly fail.

## The Environment

You are likely running a modern Linux setup (e.g., Arch, Fedora, Ubuntu 24.04) with a recent Docker Engine.

```bash
docker --version
# Output example: Docker version 29.1.2, build 890dcca
````

## The Error

When running `mvn test` (or `gradle test`), the build fails during the container startup phase. The logs will display a
`BadRequestException`:

```text
[main] ERROR org.testcontainers.dockerclient.DockerClientProviderStrategy - Could not find a valid Docker environment. Please check configuration. Attempted configurations were:
	UnixSocketClientProviderStrategy: failed with exception BadRequestException (Status 400: {"message":"client version 1.32 is too old. Minimum supported API version is 1.44, please upgrade your client to a newer version"}
)
	DockerDesktopClientProviderStrategy: failed with exception NullPointerException (Cannot invoke "java.nio.file.Path.toString()" because the return value of "org.testcontainers.dockerclient.DockerDesktopClientProviderStrategy.getSocketPath()" is null)As no valid configuration was found, execution cannot continue.
See https://java.testcontainers.org/on_failure.html for more details.
```

## The Root Cause

**Docker Engine v29** (and newer) has deprecated and removed support for older API versions. It strictly enforces a
minimum API version of **1.44**.

**Testcontainers** uses the `docker-java` library internally. To ensure maximum compatibility with older enterprise
systems, older versions of Testcontainers often attempt to negotiate using an older API version (like 1.32) during the
initial handshake.

Modern Docker Engines reject this initial "old" handshake immediately with a `400 Bad Request`, causing the test suite
to crash before negotiation can upgrade the connection.

## The Solution

While the long-term fix is to upgrade Testcontainers to the latest version, you might not want to risk breaking existing
tests or changing dependencies right now.

Fortunately, you can force the underlying Docker Java client to use a compatible API version via a configuration file.

### Step 1: Create the Configuration File

You need to create a `.docker-java.properties` file in your user's home directory. This file overrides the default
version negotiation.

Run the following command in your terminal. **Note:** We use >> to append to the file in case you already have existing
configurations.

```bash
echo "api.version=1.44" >> ~/.docker-java.properties
```

This forces the client to skip the lower-version handshake and communicate directly using API 1.44.

### Step 2: Verify

Run the tests again.

```bash
mvn clean test
```

Testcontainers will now skip the negotiation phase. You should see successful connection logs similar to this:

```text
  Server Version: 29.1.2
  API Version: 1.52
  Operating System: Ubuntu 24.04.3 LTS
```