# Chargewise RAG

AI-powered chargeback and dispute resolution copilot. It recommends a dispute category and the required evidence, cites the scheme rules it used, and refuses when the rules don't support an answer.

All scheme rules in this project are fictional.

## How it works

**Load rules**
- Upload a rules document (PDF, DOCX, TXT) with its reason category
- Apache Tika parses it, `TokenTextSplitter` chunks it, and each chunk is tagged with `documentId` and `reasonCategory`
- Chunks are embedded with OpenAI and stored in pgvector, asynchronously on virtual threads (PENDING, COMPLETED, FAILED)
- Deleting a document removes its row and all its chunk embeddings in one transaction
- Five fictional rule files, one per category, are loaded on first start

**Analyse a dispute**
- The dispute description is matched against rule chunks with pgvector similarity search
- OpenAI returns structured output mapped to a Java record: category, required evidence, explanation, citations
- Returns 422 when no rules match closely enough, instead of guessing
- Each result is stored and can be fetched by ID

## Tech stack

Java 25, Spring Boot 4, Spring AI 2, OpenAI, PostgreSQL with pgvector, Flyway, Docker Compose, Testcontainers.

## Quick start

```bash
cp .env.example .env        # set OPENAI_API_KEY
docker compose up -d
mvn spring-boot:run
```

Sample requests are in `requests.http`.

## API

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/v1/documents` | Upload a rules document (multipart), returns 202 |
| GET | `/api/v1/documents` | List documents and ingestion status, paginated |
| DELETE | `/api/v1/documents/{id}` | Delete a document and its vectors |
| POST | `/api/v1/disputes/analyse` | Analyse a dispute |
| GET | `/api/v1/disputes/{id}` | Get a stored analysis result |

Example:

```json
POST /api/v1/disputes/analyse
{ "description": "Customer says the parcel never arrived" }
```

```json
{
  "id": "4f1c...",
  "description": "Customer says the parcel never arrived",
  "category": "GOODS_NOT_RECEIVED",
  "requiredEvidence": ["Cardholder statement describing the order and the expected delivery date", "..."],
  "explanation": "...",
  "citations": [{ "document": "goods-not-received.txt", "excerpt": "..." }],
  "createdAt": "2026-11-11T22:30:00Z"
}
```

## Build and test

```bash
mvn verify
```

Tests run against a real pgvector container through Testcontainers, so Docker must be running.
