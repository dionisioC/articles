#!/bin/sh

: "${MAX_RAM_PERCENTAGE:=75.0}"

export JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=${MAX_RAM_PERCENTAGE} -Djava.security.egd=file:/dev/./urandom"

echo "Starting application with JAVA_TOOL_OPTIONS: $JAVA_TOOL_OPTIONS"
exec java org.springframework.boot.loader.launch.JarLauncher "$@"