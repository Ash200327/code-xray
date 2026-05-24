# Codebase Assistant

Production-oriented AI developer assistant for GitHub repositories.

## Stack
- Backend: Java 21, Spring Boot (WebFlux), Spring AI, Spring Security, Flyway
- Frontend: React, TypeScript, Vite, Tailwind
- Storage: PostgreSQL + PGVector
- AI: OpenAI chat + embeddings

## Key Features
- Repository ingestion with smart filtering and language-aware chunking
- Async ingestion jobs with retries and real-time progress via WebSocket/STOMP
- RAG chat with conversation memory and persisted history
- Hybrid retrieval (vector + keyword full-text merge/rerank)
- Citation transparency (source type, confidence, scores, reason)
- Workspace/repository/conversation APIs with user scoping
- JWT auth with refresh token rotation
- Repository summary + documentation generation endpoints

## Prerequisites
- Docker Desktop
- Java 21+
- Maven 3.9+
- Node.js 18+
- OpenAI API key

## 1) Configure Environment

### Windows PowerShell (current shell)
```powershell
$env:OPENAI_API_KEY="sk-..."
```

### Optional: enable strict auth mode
- Default local mode is permissive (`codeassistant.security.enabled=false` in `application.yml`)
- Strict mode is enabled in `application-prod.yml`

Use strict mode at runtime:
```bash
cd codebase-assistant-api
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

## 2) Start Database (PGVector)
```bash
docker compose up -d
```

Postgres is exposed at `localhost:5433`.

## 3) Run Backend
```bash
cd codebase-assistant-api
mvn spring-boot:run
```

Backend URL: `http://localhost:8080`

## 4) Run Frontend
```bash
cd codebase-assistant-ui
npm install
npm run dev
```

Frontend URL: `http://localhost:5173`

## 5) Build / Test

### Backend
```bash
cd codebase-assistant-api
mvn test
```

### Frontend
```bash
cd codebase-assistant-ui
npm run build
```

## Auth Endpoints
- `POST /api/auth/signup`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `GET /api/auth/me`

## Core API Surface
- Ingestion:
  - `POST /api/ingest` (legacy sync)
  - `POST /api/ingestion/jobs`
  - `GET /api/ingestion/jobs`
  - `GET /api/ingestion/jobs/{jobId}`
  - `POST /api/ingestion/jobs/{jobId}/retry`
- Chat:
  - `GET /api/chat/stream?question=...&repoUrl=...&conversationId=...`
- Workspaces / Repositories / Conversations:
  - CRUD endpoints under `/api/workspaces`, `/api/repositories`, `/api/conversations`
- Insights:
  - `GET /api/repositories/{repositoryId}/summary`
  - `GET /api/repositories/{repositoryId}/docs`

## Notes
- Flyway migrations run automatically on backend startup.
- In strict mode, scoped APIs require valid JWT bearer tokens.
- Frontend includes token refresh and protected app shell flow.
