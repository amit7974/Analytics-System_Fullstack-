# User Analytics + Semantic Search System

Spring Boot 3 / Java 17 backend combining a user-event analytics pipeline with
AI-powered semantic search over text documents, backed by MySQL.

## Architecture

```
controller/   REST endpoints (validation, HTTP concerns only)
service/      Business logic
service/embedding/   Pluggable embedding providers (local hashing | OpenAI)
repository/   Spring Data JPA repositories
model/        JPA entities
dto/          Request/response payloads
exception/    Centralized error handling (@RestControllerAdvice)
util/         Vector math (cosine similarity, JSON (de)serialization)
```

### Why this design

- **Layered architecture**: controllers never touch repositories directly; all
  business logic and validation live in the service layer.
- **Semantic search without a vector database**: MySQL doesn't have a
  first-class vector column in widely-deployed versions, so embeddings are
  stored as JSON text (`search_document.embedding`) and cosine similarity is
  computed in the application layer. This is a standard, honest approach for
  small-to-medium corpora. For large-scale production use, swap the storage
  layer for pgvector / OpenSearch k-NN / Milvus / Pinecone behind the same
  `SemanticSearchService` interface — nothing else needs to change.
- **Pluggable embeddings**: `EmbeddingService` has two implementations,
  selected by the `embedding.provider` property:
  - `local` (default) — a deterministic feature-hashing embedder. Needs no
    API key, no internet access, and no billing, so the project runs
    out-of-the-box. It's a real, well-known technique (used by e.g. Vowpal
    Wabbit), not a mock — it genuinely captures token-overlap similarity.
  - `openai` — calls the real OpenAI Embeddings API
    (`text-embedding-3-small`) for true semantic embeddings. Requires
    `OPENAI_API_KEY`.
  - Each stored document records which provider created its embedding, so
    switching providers never silently compares incompatible vector spaces.

## Requirements

- Java 17+
- Maven 3.8+
- MySQL 8+ (or nothing — see the zero-setup H2 mode below)

## Running

### Option A — zero setup (H2 in-memory, no MySQL install needed)

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

### Option B — MySQL

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS analytics_search;"

export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=analytics_search
export DB_USERNAME=root
export DB_PASSWORD=yourpassword

mvn spring-boot:run
```

Tables are auto-created on startup via `spring.jpa.hibernate.ddl-auto=update`.

### Optional — real semantic embeddings via OpenAI

```bash
export EMBEDDING_PROVIDER=openai
export OPENAI_API_KEY=sk-...
mvn spring-boot:run
```

### Build a runnable jar

```bash
mvn clean package
java -jar target/analytics-search.jar --spring.profiles.active=h2
```

### Run the tests

```bash
mvn test
```

Tests include: pure unit tests for the cosine-similarity math, unit tests
proving the local embedder scores related text higher than unrelated text,
a Spring context smoke test, and MockMvc end-to-end tests that create a
user, track events, fetch analytics, index documents, and run a semantic
search query — all against in-memory H2.

## API Reference

### Create a user

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","email":"alice@example.com"}'
```

### Track an event

```bash
curl -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -d '{
        "userId": 1,
        "eventType": "page_view",
        "metadata": {"page": "/checkout", "device": "mobile"}
      }'
```

### Per-user analytics

```bash
curl http://localhost:8080/api/analytics/users/1
```

Returns total event count, a breakdown by event type, first/last event
timestamps, and the 20 most recent events.

### Global aggregate analytics

```bash
curl "http://localhost:8080/api/analytics/events/aggregate?from=2026-06-01T00:00:00Z&to=2026-07-01T00:00:00Z"
```

`from`/`to` are optional and default to the last 30 days.

### Index a document for semantic search

```bash
curl -X POST http://localhost:8080/api/search/index \
  -H "Content-Type: application/json" \
  -d '{"content": "Spring Boot REST API for real-time event tracking and analytics"}'
```

### Run a semantic search

```bash
curl -X POST http://localhost:8080/api/search/semantic \
  -H "Content-Type: application/json" \
  -d '{"query": "analytics dashboard API", "topK": 5}'
```

Returns documents ranked by cosine similarity score (0–1), highest first.

## Configuration reference (`application.yml`)

| Property | Env var | Default | Purpose |
|---|---|---|---|
| `spring.datasource.url` | `DB_HOST`, `DB_PORT`, `DB_NAME` | `localhost:3306/analytics_search` | MySQL connection |
| `spring.datasource.username/password` | `DB_USERNAME`, `DB_PASSWORD` | `root`/`root` | MySQL credentials |
| `embedding.provider` | `EMBEDDING_PROVIDER` | `local` | `local` or `openai` |
| `embedding.dimension` | `EMBEDDING_DIMENSION` | `384` | Local embedder vector size |
| `openai.api-key` | `OPENAI_API_KEY` | _(empty)_ | Required only if `embedding.provider=openai` |

## Notes on scope / known limitations (being upfront)

- Semantic search does a brute-force scan over all stored documents. Fine for
  thousands of documents; a real vector index is needed beyond that.
- No authentication/authorization layer — add Spring Security if this is
  exposed beyond a private network.
- The local embedder is a legitimate baseline (bag-of-words via feature
  hashing), not a neural embedding model — it won't capture synonyms or deep
  semantics the way `text-embedding-3-small` does. Use the OpenAI provider
  for genuinely semantic results.
