# ---- Etapa 1: compilar ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Primero solo el pom
COPY pom.xml .
RUN mvn dependency:go-offline

# Ahora el código y la compilación
COPY src ./src
RUN mvn clean package -DskipTests

# ---- Etapa 2: ejecutar ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Solo se copia el .jar
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]