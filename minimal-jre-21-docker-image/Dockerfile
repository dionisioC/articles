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
