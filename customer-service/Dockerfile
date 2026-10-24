# ---------- Etapa 1: compilar ----------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Primero solo lo necesario para descargar dependencias (se guarda en caché)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Luego el código fuente y la compilación
COPY src ./src
RUN ./mvnw package -DskipTests -B

# ---------- Etapa 2: ejecutar ----------
FROM eclipse-temurin:25-jre
WORKDIR /app

# curl se instala solo para el HEALTHCHECK; se borra la caché de apt para no engordar la imagen
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app && useradd --system --gid app app

COPY --from=build /app/target/*.jar app.jar

USER app
EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -fs http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]