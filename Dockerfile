FROM eclipse-temurin:25-jre-alpine@sha256:2ca9adf44f5c29d28ecd26cf92d75cc0c66b7f32bfd839a4439e363a8b428af8

RUN addgroup -g 150 -S apprunner \
 && adduser -u 150 -S apprunner -G apprunner

COPY --chown=150:150 build/install/matrikkel-ekstern-data-ingestor /app

USER apprunner
EXPOSE 8090

ENTRYPOINT ["/app/bin/matrikkel-ekstern-data-ingestor"]
