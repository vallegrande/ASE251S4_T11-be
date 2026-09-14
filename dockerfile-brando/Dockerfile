# ---------- STAGE 1: Build ----------
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copiar pom y descargar dependencias (cache)
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

# Copiar código fuente y compilar
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# ---------- STAGE 2: Runtime ----------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Crear usuario no-root por seguridad
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copiar el JAR generado
COPY --from=builder /app/target/*.jar app.jar

# Puerto de la aplicación
EXPOSE 8080

# Variables de entorno por defecto (se sobreescriben en Kubernetes con Secret)
ENV SPRING_PROFILES_ACTIVE=default
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# Healthcheck simple usando una ruta disponible en la API REST
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -q --spider http://localhost:8080/api/customers || exit 1 || true

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
