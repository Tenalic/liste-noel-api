# Étape 1 : récupération + build du front
FROM node:20-alpine AS frontend-build
RUN apk add --no-cache git
RUN git clone --depth 1 https://github.com/Tenalic/gift-list-manager.git /front
WORKDIR /front
RUN npm ci
RUN npm run build

# Étape 2 : build du back (ton étape actuelle, inchangée)
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src/ ./src/
# On copie le build React généré à l'étape 1 dans les ressources statiques AVANT le package
COPY --from=frontend-build /front/dist ./src/main/resources/static
RUN mvn clean package -DskipTests

# Étape 3 : image finale légère (inchangée)
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/liste-noel-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]