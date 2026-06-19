# ============ Etapa 1: Build ============
# Usamos una imagen con Maven + JDK 17 para compilar el proyecto.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Primero copiamos solo el pom.xml para cachear las dependencias.
# Si el pom no cambia, Docker reutiliza esta capa y la build es más rápida.
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Ahora copiamos el código fuente y empaquetamos el .jar.
COPY src ./src
RUN mvn clean package -DskipTests -B

# ============ Etapa 2: Runtime ============
# Imagen ligera, solo con el JRE necesario para ejecutar (no incluye Maven).
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copiamos el .jar generado en la etapa de build.
COPY --from=build /app/target/*.jar app.jar

# Documentamos el puerto (el real lo define la variable PORT en el hosting).
EXPOSE 8080

# Arrancamos la aplicación.
ENTRYPOINT ["java", "-jar", "app.jar"]