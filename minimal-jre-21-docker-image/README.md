# Minimal JRE 21 Docker Image

A minimal, efficient, and secure container image is not something desirable; it is a fundamental requirement as is it
something that we are going to download multiple times and the smaller, the smaller the possibility of having
vulnerabilities (less size, fewer packages). Resource efficiency directly impacts in hosting costs, application startup
speed dictates the agility of scaling the containers, and a minimized attack surface is an essential thing for a robust
security posture.

## Overview of the optimized container

1. **Build-Time Layering:** We are going to use Spring Boot's layered JAR feature. This aligns the internal structure of
   the application artifact with Docker's layer caching mechanism. By separating infrequently changing dependencies from
   volatile application code, we are going to accelerate subsequent builds and reduce the bandwidth required for image
   distribution, directly enhancing CI/CD pipeline velocity and developer experience.

2. **Runtime Minimization:** We are going to create a tailored, minimal JRE using the `jlink` tool.
   Instead of deploying a full, general-purpose JDK or JRE, we are going to analyze the application's specific module
   requirements and construct a runtime containing only the necessary components. This results in a reduction
   in the final image size and, critically, its security footprint by eliminating unused code that could have
   vulnerabilities.

3. **Runtime Resilience:** We are going to address the operational stability of the application within a container
   orchestration environment. We will implement a container-aware entrypoint script that dynamically configures JVM
   memory parameters. By using modern JVM flags like `-XX:MaxRAMPercentage` instead of legacy, static flags, the
   application can intelligently adapt to the memory limits imposed by the orchestrator. This practice is essential for
   preventing the common `OOMKilled` (Out of Memory Killed) errors that sometimes we have in misconfigured Java
   applications in containerized deployments and is not hard to address.

## Spring Boot 3 Kotlin Application

We are going to have a simple application that is only a regular springboot application with a controller that returns
"hello world". For the sake of simplicity we are going to omit this steps, but if you want to check the app, it is good
to know that it is a simple app.

## Spring Boot's Layered Jars

### The Problem with the Fat JAR

By default, the `spring-boot-maven-plugin` packages the application into a single, executable "fat JAR" (or "uber JAR").
This artifact is self-contained and easy to run, as it bundles the application's compiled classes along with all of its
dependencies into one file. While convenient for traditional deployments, this monolithic structure is inefficient in a
containerized workflow.

The core issue lies in how Docker builds images. Any change to a file within a layer invalidates that layer and all
subsequent layers in the cache. For a fat JAR, the entire artifact exists in a single `COPY` layer in the `Dockerfile`.
This means that even a one-line code change forces Docker to treat the entire JAR as a new file. Consequently, this
massive layer is invalidated, and the entire artifact, including all its unchanged dependencies, must be rebuilt and
re-uploaded to the container registry during every single build. This process is slow, wastes network bandwidth, and
consumes unnecessary storage.

### Aligning with Docker's Layer Cache

The solution is to structure the application artifact in a way that aligns with Docker's layered filesystem. The goal is
to isolate components based on their rate of change. Infrequently changing, heavyweight components like third-party
library dependencies should be placed in lower, stable layers that are cached early and rarely invalidated. Frequently
changing, lightweight components: Components like the application code should be placed in higher, more volatile layers.
This arrangement ensures that when you modify the code, only the smaller application layer is rebuilt, while the larger
dependency layers stay cached, resulting in significantly faster build times.

### Enabling Layered Jars in Maven

Spring Boot, starting from version 2.3, provides a first-class solution to this problem with its "layered JAR" feature.
Enabling it is a simple configuration change within the `spring-boot-maven-plugin` section of the `pom.xml`:

```xml

<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <layers>
            <enabled>true</enabled>
        </layers>
    </configuration>
</plugin>
```

With this configuration, running `mvn package` will still produce a single executable JAR, but it will now contain
internal metadata that logically partitions its contents into layers.

### Checking the Default Layers

When the `layers` feature is enabled, the plugin embeds a `BOOT-INF/layers.idx` file within the JAR. This index file
defines the mapping of the JAR's contents to specific layers. We can inspect this structure using the `tools`
mode. After building the project, we can run the following command:

`java -Djarmode=tools -jar target/minimal-jre-21-0.0.1-SNAPSHOT.jar list-layers`

This command will output the names of the layers in the order they should be added to a Docker image to maximize caching
efficiency. The default layers are:

1. **`dependencies`**: This layer contains all non-SNAPSHOT library dependencies. It is typically the largest and most
   stable layer, changing only when a dependency is added, removed, or updated in the `pom.xml`.
2. **`spring-boot-loader`**: This layer contains the classes required to launch a layered JAR. It is extremely stable
   and only changes with a Spring Boot version upgrade.
3. **`snapshot-dependencies`**: This layer contains any SNAPSHOT dependencies. These are, by definition, volatile and
   are separated to avoid invalidating the main `dependencies` layer.
4. **`application`**: This layer contains the compiled classes and resources of the application, located in
   `BOOT-INF/classes` and `BOOT-INF/lib` for local module dependencies. Although this layer is copied last in the
   Dockerfile, it is the most frequently changed layer. By placing it at the end of the build process, any changes to
   the application code only require rebuilding this layer, allowing the larger, more stable layers below it (such as
   the dependencies and the Spring Boot loader) to remain cached. This significantly speeds up the build process.

By adopting this layered structure, the build process becomes significantly more efficient. A developer changing only
their application code will find that their CI/CD pipeline only needs to rebuild and push the small `application` layer,
rather than the entire hundred-megabyte artifact. This optimization dramatically shortens the feedback loop, making
iterative development and frequent deployments faster and more cost-effective (everyone reading this loves TDD and fast
feedback loops right?).

## Runtime Optimization, Minimal JRE with JLink

While layering optimizes the build process, `jlink` optimizes the final runtime artifact. Standard container base
images, such as `eclipse-temurin:21-jdk` or even `eclipse-temurin:21-jre`, are designed for general-purpose use. They
contain the full Java Development Kit or a comprehensive Java Runtime Environment, including numerous modules and tools
that a typical, headless web service will never use.

The standard containers have two significant downsides:

1. **Increased Size**: A full JDK image can be several hundred megabytes larger than necessary, leading to slower image
   pulls during container startup, increased network transfer costs, and higher storage consumption in the registry and
   on cluster nodes.
2. **Expanded Attack Surface**: Every unused library and executable included in the image is a potential source of
   security vulnerabilities (CVEs). By eliminating these components, we reduce the application's attack surface,
   creating a more secure production environment.

The `jlink` tool, introduced in Java 9 as part of Project Jigsaw, provides the solution by enabling the creation of
custom, minimal JREs tailored to the specific needs of an application.

### The `jdeps` -\> `jlink` Workflow

The process of creating a custom JRE involves two key command-line tools from the JDK:

1. **`jdeps` (Java Dependency Analysis Tool)**: The first step is to determine exactly which JDK modules the application
   and its dependencies require. `jdeps` analyzes Java bytecode (`.class` or `.jar` files) and reports these module
   dependencies.
2. **`jlink` (Java Linker)**: Once the list of required modules is known, `jlink` takes this list as input and assembles
   a custom runtime image. This image contains only the specified modules and their transitive dependencies, resulting
   in a minimal, self-contained JRE.

### Analyzing a Spring Boot Fat JAR

A significant challenge arises when trying to apply this workflow to a standard Spring Boot application: `jdeps` is
designed to work with modular or non-modular JARs on a classpath, but it struggles to correctly analyze the nested
structure of a fat JAR. The dependencies are located inside the `BOOT-INF/lib` directory within the main JAR, a
structure that `jdeps` does not natively understand.

The solution is to use the `layertools`feature from the previous step not just for caching, but as a preparatory step
for `jlink`. By extracting the layered JAR, we create a standard filesystem structure that `jdeps` can easily parse.

The command to perform this extraction is:
`java -Djarmode=tools -jar target/minimal-jre-21-0.0.1-SNAPSHOT.jar extract --layers --launcher`

This command unpacks the JAR into a directory structure that mirrors the layers, for example, creating `dependencies/`,
`application/`, etc. We can now run `jdeps` against these directories to get an accurate list of required JDK
modules.

### Identifying Required Modules for a Spring Web App

With the application extracted, we can now run `jdeps` to discover the necessary modules. The command points to the
application's classes and all the dependency JARs:

`jdeps --multi-release 21 --ignore-missing-deps --print-module-deps --class-path dependencies/BOOT-INF/lib/* application/BOOT-INF/classes`

Inside the previously unpacked directory.

This will produce a comma-separated list of required modules. For a typical Spring Boot web application, this list will
include several modules whose purpose may not be immediately obvious. The `jlink` command then uses this list to build
the runtime, along with flags to further reduce its size:

The output is the following:

`java.base,java.compiler,java.desktop,java.instrument,java.management,java.naming,java.net.http,java.prefs,java.rmi,java.scripting,java.security.jgss,java.sql,jdk.jfr,jdk.unsupported`

The following table breaks down the common modules required for a Spring Boot 3 web service and justifies their
inclusion. This transforms the potentially opaque output of `jdeps` into a clear and understandable checklist.

| Module Name          | Required For          | Justification & Rationale                                                                                                                                                                                                                                                                                                                   |
|:---------------------|:----------------------|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `java.base`          | Core Java Platform    | The fundamental module containing essential classes like `java.lang.Object`, `java.lang.String`, and core collections. Every Java application requires this.                                                                                                                                                                                |
| `java.logging`       | Logging Frameworks    | Required by logging facades like SLF4J and implementations like Logback or Log4j2, which are standard in Spring Boot applications.                                                                                                                                                                                                          |
| `java.management`    | JMX and Monitoring    | Essential for the Java Management Extensions (JMX) framework. Spring Boot Actuator heavily uses JMX to expose metrics, health endpoints, and management beans.                                                                                                                                                                              |
| `java.sql`           | Database Connectivity | Provides the core JDBC API (`java.sql.Connection`, etc.). Even if the application doesn't connect to a database directly, a transitive dependency (e.g., a connection pool like HikariCP) will require this module.                                                                                                                         |
| `java.naming`        | JNDI                  | Provides the Java Naming and Directory Interface (JNDI) API. This is often used by application servers and resource managers (like connection pools) for resource lookups.                                                                                                                                                                  |
| `java.instrument`    | JVM Instrumentation   | Allows agents to instrument programs running on the JVM. This can be used by various tools for profiling, monitoring, or bytecode manipulation.                                                                                                                                                                                             |
| `java.security.jgss` | Advanced Security     | Provides the Java binding for the Generic Security Service API (GSS-API), often used for Kerberos-based authentication mechanisms. It can be pulled in by security-related dependencies.                                                                                                                                                    |
| `java.desktop`       | AWT/Swing (Indirect)  | Often a surprising inclusion for a headless server. It is typically pulled in transitively by libraries that have optional dependencies on Abstract Window Toolkit (AWT) for tasks like image processing or font metrics, even if those features are not used.                                                                              |
| `jdk.unsupported`    | Unsafe Operations     | This critical module provides access to low-level, internal JVM APIs like `sun.misc.Unsafe`. High-performance frameworks, including Spring and Netty, use these APIs for direct memory access and other optimizations that bypass standard Java safety checks. Its inclusion is mandatory for many modern frameworks to function correctly. |

This `jlink` process acts as a powerful audit of the application's true runtime requirements. By forcing a declaration
of all necessary modules, it encourages a deeper understanding of the application's dependency graph and provides a
significant step forward in creating a truly minimal and secure production artifact.

## Multi-Stage Dockerfile

### Multi-Stage Builds

A multi-stage build is an important method for making container images that are both optimized and secure. It involves
using multiple `FROM` instructions within a single `Dockerfile`. Each `FROM` statement initiates a new, temporary build
stage with its own base image and set of commands. The key principle is that only artifacts explicitly copied from a
previous stage into the final stage (using `COPY --from=<stage_name>`) are included in the final production image.

This technique allows for a clean separation of concerns. We can use a large, tool-rich "builder" image (e.g., one
containing the full JDK and Maven) to compile our code and run complex build steps. The final "runtime" image, however,
can be based on a minimal OS, containing only the compiled application and its minimal JRE, completely discarding all
build-time tools, source code, and intermediate artifacts. This dramatically reduces the final image size and
enhances security by ensuring no build tools are present in the production environment.

### The Complete `Dockerfile`

The `Dockerfile` below combines everything mentioned earlier, including `jlink` and runtime configuration, into one
unified
build process.

```dockerfile
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jdk-alpine AS jre-crafter

WORKDIR /app

COPY --from=builder /app/target/*.jar application.jar

RUN java -Djarmode=tools -jar application.jar extract --layers --launcher  &&\
    jdeps \
    --multi-release 21 \
    --ignore-missing-deps \
    --print-module-deps \
    --class-path application/dependencies/BOOT-INF/lib/* \
    application/application/BOOT-INF/classes > jre-deps.info &&  \
    jlink \
    --add-modules $(cat jre-deps.info) \
    --strip-debug \
    --compress 2 \
    --no-header-files \
    --no-man-pages \
    --output /custom-jre

FROM alpine:3

RUN addgroup -S appuser && adduser -S appuser -G appuser

ENV JAVA_HOME=/opt/jre
ENV PATH="$JAVA_HOME/bin:$PATH"

WORKDIR /app

COPY --from=jre-crafter /custom-jre $JAVA_HOME

COPY --from=jre-crafter /app/application/dependencies/ ./
COPY --from=jre-crafter /app/application/spring-boot-loader/ ./
COPY --from=jre-crafter /app/application/snapshot-dependencies/ ./
COPY --from=jre-crafter /app/application/application/ ./

COPY entrypoint.sh .

RUN chmod +x entrypoint.sh && chown -R appuser:appuser /app && chown -R appuser:appuser /opt/jre

USER appuser

ENTRYPOINT ["./entrypoint.sh"]
```

## The Function of an entrypoint.sh Script

While a `Dockerfile` can use a simple `CMD` or `ENTRYPOINT` instruction to launch the Java application, a dedicated
shell script (`entrypoint.sh`) provides a powerful mechanism for adding dynamic, runtime configuration. This is
indispensable in production environments like Kubernetes, where an application must adapt to settings provided by the
orchestrator at launch time, such as memory limits or configuration profiles. The entrypoint script acts as
a bootstrapper, preparing the environment before handing control over to the main application process.

### The `exec` Command for Graceful Shutdowns

When a script runs a command like `java -jar app.jar`, the shell starts the Java process as a child. The shell itself
remains the main process (PID 1) inside the container. When Kubernetes decides to terminate the pod, it sends a
`SIGTERM` signal to PID 1 (the shell). The shell may not be configured to propagate this signal to its child Java
process.
After a timeout (the `terminationGracePeriodSeconds`), Kubernetes will forcefully kill the container with `SIGKILL`,
preventing the application from performing a graceful shutdown (e.g., finishing in-flight requests, closing database
connections, writing final logs).

The `exec java...` command completely changes this behavior. The `exec` command *replaces* the current shell process
with the new command. This means the Java process itself becomes PID 1. Now, when Kubernetes sends `SIGTERM`, it is
delivered directly to the JVM, which correctly initiates its shutdown hooks, allowing for a clean and graceful exit.
This is a non-negotiable best practice for any production container.

### Dynamically Configuring JVM Memory

The entrypoint script is the ideal place to translate environment variables provided by the orchestrator into JVM
configuration flags. The standard, non-intrusive way to pass options to any JVM is via the `JAVA_TOOL_OPTIONS`
environment variable. The JVM automatically parses this variable and applies the specified flags upon startup.
This decouples the static Docker image from its runtime configuration, allowing the same image to be deployed with
different memory settings without being rebuilt.

### The Complete `entrypoint.sh`

The following script implements these best practices. It sets a default for the heap percentage but allows it to be
overridden by an environment variable, configures `JAVA_TOOL_OPTIONS`, and uses `exec` to launch the application
correctly.

```bash
#!/bin/sh

: "${MAX_RAM_PERCENTAGE:=75.0}"

export JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=${MAX_RAM_PERCENTAGE} -Djava.security.egd=file:/dev/./urandom"

echo "Starting application with JAVA_TOOL_OPTIONS: $JAVA_TOOL_OPTIONS"
exec java org.springframework.boot.loader.launch.JarLauncher "$@"
```

## Memory Tuning for Kubernetes Environments

### Java Memory is Hard

Managing Java memory effectively within the tight limits of a container is a well-known challenge that often results in
instability. The root of the problem lies in the JVM's complex memory structure and its interaction with the memory
limits set by the container, which are controlled by the kernel's cgroup system.

The total memory usage of a Java process includes more than just its heap. It can be generally categorized into:

* **Heap Memory**: This is where all application objects are allocated. Its size is directly controllable with JVM flags
  and is managed by the Garbage Collector (GC).
* **Non-Heap Memory**: This is a collection of several other memory areas, including:
    * **Metaspace**: Stores class metadata (definitions of classes and methods).
    * **Code Cache**: Stores native code compiled from Java bytecode by the Just-In-Time (JIT) compiler.
    * **Thread Stacks**: Each thread has its own stack.
    * **Direct Buffers**: Used for off-heap memory allocations, often for high-performance I/O.

The critical issue is that while the Heap size can be precisely limited, the size of the Non-Heap areas is much harder
to predict and control. However, the Kubernetes memory limit applies to the *entire process memory* (Heap + Non-Heap).
If this total usage exceeds the container's limit, the Linux Out-Of-Memory (OOM) Killer will be invoked, and Kubernetes
will terminate the pod with the dreaded `OOMKilled` status.

### Using the modern `-XX:MaxRAMPercentage` instead of the classic `-Xmx`

Historically, Java developers used the `-Xmx` flag to set a fixed, absolute maximum heap size. This approach is
fundamentally flawed in a containerized world. A modern, container-aware JVM offers a superior mechanism:
percentage-based heap sizing.

| Feature                 | `-Xmx<size>` (e.g., `-Xmx1g`)                                                                                                                                                                                                                                                              | `-XX:MaxRAMPercentage=<value>` (e.g., `-XX:MaxRAMPercentage=75.0`)                                                                                                                                                                                                                             |
|:------------------------|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Mechanism**           | Sets a **fixed, absolute** maximum heap size in bytes.                                                                                                                                                                                                                                     | Sets the maximum heap size as a **percentage** of the available container memory.                                                                                                                                                                                                              |
| **Container Awareness** | **None.** The JVM is oblivious to the container's memory limit. It will attempt to allocate the specified heap regardless of the container's constraints, often leading to immediate OOMKills if `-Xmx` is larger than the container limit.                                                | **Fully Aware.** Since JDK 8u191, the JVM (with `+UseContainerSupport` on by default) queries the kernel's cgroups to detect the container's memory limit and uses that as the basis for its calculation.                                                                                      |
| **Flexibility**         | **Brittle.** The memory setting is hardcoded into the image or launch command. To adjust the heap size, you must rebuild the image or create complex entrypoint scripts to manually calculate a new `-Xmx` value based on the container limit. This couples configuration to the artifact. | **Flexible.** The same image adapts dynamically to any container memory limit. Memory tuning is shifted to the Kubernetes deployment manifest, where it belongs. An operator can change the container's memory limit, and the JVM will automatically adjust its heap size on the next startup. |
| **Recommendation**      | **Deprecated for container use.** Its use is a common anti-pattern that leads to inflexible and unstable deployments.                                                                                                                                                                      | **Strongly Recommended.** This is the correct, modern approach for configuring JVM heap memory in any containerized environment.                                                                                                                                                               |

### Selecting a Safe `MaxRAMPercentage`

Using percentage-based sizing immediately raises the question: what percentage should be used? Setting
`-XX:MaxRAMPercentage=100.0` is a common mistake that guarantees instability, as it leaves zero memory for all the
essential Non-Heap components.

A safe starting point could be **75% to 80%**.
This allocates a majority of the container's memory to the application's heap while reserving a 20-25% buffer for
Metaspace, the code cache, thread stacks, and other native memory requirements. For smaller containers (e.g., \< 1 GB),
starting closer to 75% is prudent. For larger containers, one might be able to push closer to 80%. This value should be
considered a starting point, to be refined by observing the application's actual memory usage under load using an
Application Performance Monitoring (APM) tool.
