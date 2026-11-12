# Changelog

All notable changes to this project are documented in this file. The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project follows [Semantic Versioning](https://semver.org/).

## [1.0.0] - 2026-11-12

### Added
- Multi-stage Dockerfile using Spring Boot layered jars and a non-root runtime user
- Application service in Docker Compose, so `docker compose up` runs the API and the database together
- GitHub Actions CI that builds, runs all tests, uploads the coverage report and builds the Docker image on every push and pull request to `main`
- JaCoCo coverage gate that fails the build below 85% instruction coverage

## [0.6.0] - 2026-11-12

### Added
- Unit tests for dispute analysis, idempotency, resilience, rate limiting and citation excerpts
- Integration tests for the full API against PostgreSQL with pgvector (Testcontainers)
- WireMock in place of the OpenAI chat API and a deterministic embedding model, so tests need no API key
- JaCoCo coverage report

## [0.5.0] - 2026-11-12

### Added
- `Idempotency-Key` header on dispute analysis: a repeated key returns the stored result with `Idempotent-Replayed: true`
- `409` when a key is reused with a different request or is still being processed
- Per-user rate limit of 10 analyses per minute with Bucket4j, returning `429` with `Retry-After`

## [0.4.0] - 2026-11-12

### Added
- Resilience4j retry with exponential backoff, circuit breaker and time limiter around every OpenAI call
- `503` with `Retry-After` when OpenAI is unavailable, slow or the circuit is open

## [0.3.0] - 2026-11-12

### Added
- `POST /api/v1/auth/login` issuing JWT access tokens
- `ADMIN` role for documents and `AGENT` role for disputes
- `401` and `403` as `ProblemDetail` with a correlation ID
- Bearer authentication in Swagger UI

### Fixed
- Citations are resolved from source labels while explanations refer to rule sections

## [0.2.0] - 2026-11-12

### Added
- Global exception handling with RFC 9457 `ProblemDetail` and field-level validation errors
- `X-Correlation-Id` on every response and in every log line, including background ingestion
- Swagger UI and OpenAPI documentation

## [0.1.0] - 2026-11-11

### Added
- Upload, list and delete rules documents (PDF, DOCX, TXT)
- Asynchronous ingestion with Apache Tika, token chunking and OpenAI embeddings in pgvector
- Dispute analysis with similarity search, context expansion by reason category and structured output
- Refusal guard returning `422` when no rules match
- Fictional seed rules, Postman collection and `requests.http`

[1.0.0]: https://github.com/dusmantajena/chargewise-rag/compare/v0.6.0...v1.0.0
[0.6.0]: https://github.com/dusmantajena/chargewise-rag/compare/v0.5.0...v0.6.0
[0.5.0]: https://github.com/dusmantajena/chargewise-rag/compare/v0.4.0...v0.5.0
[0.4.0]: https://github.com/dusmantajena/chargewise-rag/compare/v0.3.0...v0.4.0
[0.3.0]: https://github.com/dusmantajena/chargewise-rag/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/dusmantajena/chargewise-rag/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/dusmantajena/chargewise-rag/releases/tag/v0.1.0
