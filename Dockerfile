FROM eclipse-temurin:25-jre-alpine@sha256:3137541deb3cac6626b5d9a4a2187bc0d6a34312f858bd2c67dd01e732e6b682

RUN addgroup -g 150 -S apprunner \
 && adduser -u 150 -S apprunner -G apprunner

COPY --chown=150:150 build/install/matrikkel-ekstern-data-ingestor /app

USER apprunner
EXPOSE 8090

ENTRYPOINT ["/app/bin/matrikkel-ekstern-data-ingestor"]
