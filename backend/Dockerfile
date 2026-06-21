## Stage 1: Build and Extract Layers
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /build

# 1. Copy ONLY the wrapper and pom.xml first to take advantage of the dependency cache
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# 2. Now We copy the source code and compile
COPY src ./src
RUN ./mvnw clean package -DskipTests

# 4. Spring Boot Magic: Copy the compiled application
RUN cp target/*.jar application.jar

# 5. Spring Boot Magic: Extracting the Layers
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

## Stage 2: Secure and Optimized Runtime
FROM eclipse-temurin:25-jre-alpine AS runtime
WORKDIR /app

# 1. Security: Install tzdata and Create and use a user without privileges
RUN apk add --no-cache tzdata && addgroup -S spring && adduser -S spring -G spring

# 2. Copy the Spring Boot layers in order volatility
COPY --from=builder --chown=spring:spring /build/extracted/dependencies/ ./
COPY --from=builder --chown=spring:spring /build/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=spring:spring /build/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=spring:spring /build/extracted/application/ ./

# Switch to secure user
USER spring:spring

# 3. JVM Configuration for Dynamic Containers
ENV JAVA_TOOL_OPTIONS="-XX:+UseG1GC -XX:+UseStringDeduplication -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

# 4. Run through the layer launcher
ENTRYPOINT ["sh", "-c", "java -jar application.jar"]