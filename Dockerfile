FROM eclipse-temurin:25-jre-alpine@sha256:c707c0d18cb9e8556380719f80d96a7529d0746fbb42143893949b98ed2f8943

RUN addgroup -g 150 -S apprunner \
 && adduser -u 150 -S apprunner -G apprunner

COPY --chown=150:150 build/install/matrikkel-ekstern-data-ingestor /app

USER apprunner
EXPOSE 8090

ENTRYPOINT ["/app/bin/matrikkel-ekstern-data-ingestor"]
