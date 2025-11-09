FROM eclipse-temurin:21-jre-jammy

RUN apt-get update && apt-get install -y \
    curl \
    iputils-ping \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd -r appuser && useradd -r -g appuser appuser

WORKDIR /app

COPY target/*.jar app.jar

RUN chown -R appuser:appuser /app
USER appuser

EXPOSE 9090

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:9090/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]