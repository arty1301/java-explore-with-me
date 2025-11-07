FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 9090

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:9090/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]