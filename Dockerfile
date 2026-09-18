FROM eclipse-temurin:25-jre-alpine@sha256:fd546f46322c4640dd5d862b1871be99c5f005720905b4c0e118dd7d0c2079aa

RUN addgroup -g 150 -S apprunner \
 && adduser -u 150 -S apprunner -G apprunner

COPY --chown=150:150 build/install/matrikkel-ekstern-data-ingestor /app

USER apprunner
EXPOSE 8090

ENTRYPOINT ["/app/bin/matrikkel-ekstern-data-ingestor"]
