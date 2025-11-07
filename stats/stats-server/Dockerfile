FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 9090

ENTRYPOINT ["java", "-jar", "app.jar"]