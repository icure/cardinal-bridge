# ---- Build stage ----
FROM --platform=linux/amd64 eclipse-temurin:21-jdk-jammy AS builder

# Install ARM64 dev libraries for cross-linking
RUN dpkg --add-architecture arm64 && \
    sed -i 's/^deb http/deb [arch=amd64] http/g' /etc/apt/sources.list && \
    echo "deb [arch=arm64] http://ports.ubuntu.com/ubuntu-ports jammy main restricted universe" >> /etc/apt/sources.list && \
    echo "deb [arch=arm64] http://ports.ubuntu.com/ubuntu-ports jammy-updates main restricted universe" >> /etc/apt/sources.list && \
    echo "deb [arch=arm64] http://ports.ubuntu.com/ubuntu-ports jammy-security main restricted universe" >> /etc/apt/sources.list && \
    apt-get update && apt-get install -y --no-install-recommends \
        libssl-dev:arm64 \
        libcurl4-openssl-dev:arm64 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /project

# Gradle wrapper first — cached until wrapper version changes
COPY gradlew ./
COPY gradle/ gradle/
RUN chmod +x gradlew

# Build definition files — cached until dependencies change
COPY settings.gradle.kts build.gradle.kts ./
COPY server/build.gradle.kts server/

# Pre-fetch Gradle distribution + Kotlin/Native toolchain (heavy, ~1 GB first run)
RUN --mount=type=cache,target=/root/.gradle \
    --mount=type=cache,target=/root/.konan \
    ./gradlew :server:dependencies --no-daemon 2>/dev/null; true

# Full source — only this layer reruns on source changes
COPY . .

# Point the Kotlin/Native cross-linker at the ARM64 multiarch libraries
RUN echo "cinteropsLibsDir=/usr/lib/aarch64-linux-gnu" > local.properties

RUN --mount=type=cache,target=/root/.gradle \
    --mount=type=cache,target=/root/.konan \
    ./gradlew :server:linkDebugExecutableLinuxArm64 --no-daemon

# ---- Runtime stage ----
FROM --platform=linux/arm64 debian:bookworm-slim

# libcurl4 pulls in libssl3 (libcrypto.so.3) automatically
RUN apt-get update && apt-get install -y --no-install-recommends \
        libgcc-s1 \
        libcurl4 \
        ca-certificates \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /project/server/build/bin/linuxArm64/debugExecutable/server.kexe /app/server

RUN chmod +x /app/server

EXPOSE 8080

ENTRYPOINT ["/app/server"]