FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="Peter Kimeli"
LABEL description="Fintech Account Management Microservice"

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY target/account-service-1.0.0.jar app.jar

RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
