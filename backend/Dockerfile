# 1. Build Phase
FROM eclipse-temurin:25-jdk-noble AS builder
WORKDIR /workspace
COPY . .

RUN ./mvnw clean package -DskipTests

# 2. Runtime Phase
FROM eclipse-temurin:25-jre-noble
# Buildpacks create a dedicated system user to run the app
RUN addgroup --system --gid 1001 cnb && adduser --system --uid 1001 --ingroup cnb cnb
USER 1001:1001

WORKDIR /workspace

COPY --from=builder /workspace/target/*.jar app.jar

# Environment settings that buildpacks automatically apply
ENV BPL_JVM_THREAD_COUNT=20
ENV BPL_JVM_LOAD_CACHED_CLASSES_FROM_HOST_MEMORY=true

ENTRYPOINT ["java", "-jar", "app.jar"]