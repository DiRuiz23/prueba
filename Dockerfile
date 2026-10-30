# Etapa 1: Compilación con JDK 17 y Gradle Wrapper
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

COPY gradlew .
COPY gradle ./gradle
COPY build.gradle settings.gradle ./
RUN chmod +x ./gradlew

# Copiar código fuente y empaquetar el JAR
COPY src ./src
RUN ./gradlew bootJar -x test --no-daemon

# Etapa 2: Imagen ligera de ejecución para producción
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

# Optimización de memoria para el plan gratuito de Render (512MB RAM)
ENV JAVA_OPTS="-Xmx384m -Xms128m -XX:+UseContainerSupport"
EXPOSE 8000

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8000} -jar app.jar"]
