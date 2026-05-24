CREATE TABLE IF NOT EXISTS workspaces (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS code_repositories (
    id UUID PRIMARY KEY,
    workspace_id UUID REFERENCES workspaces(id) ON DELETE SET NULL,
    name VARCHAR(200) NOT NULL,
    repo_url VARCHAR(500) NOT NULL,
    branch VARCHAR(120) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    last_ingested_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_code_repositories_workspace_id ON code_repositories(workspace_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_code_repositories_repo_url_branch ON code_repositories(repo_url, branch);

CREATE TABLE IF NOT EXISTS conversations (
    id UUID PRIMARY KEY,
    repository_id UUID NOT NULL REFERENCES code_repositories(id) ON DELETE CASCADE,
    title VARCHAR(250) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_conversations_repository_id ON conversations(repository_id);

CREATE TABLE IF NOT EXISTS messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    role VARCHAR(30) NOT NULL,
    content TEXT NOT NULL,
    citations_json TEXT,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_messages_conversation_id ON messages(conversation_id);
CREATE INDEX IF NOT EXISTS idx_messages_created_at ON messages(created_at);

CREATE TABLE IF NOT EXISTS ingestion_jobs (
    id UUID PRIMARY KEY,
    repository_id UUID REFERENCES code_repositories(id) ON DELETE SET NULL,
    repo_url VARCHAR(500) NOT NULL,
    branch VARCHAR(120) NOT NULL,
    status VARCHAR(30) NOT NULL,
    attempt INTEGER NOT NULL,
    max_attempts INTEGER NOT NULL,
    error_message TEXT,
    result_json TEXT,
    created_at TIMESTAMP NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ingestion_jobs_status ON ingestion_jobs(status);
CREATE INDEX IF NOT EXISTS idx_ingestion_jobs_created_at ON ingestion_jobs(created_at);

CREATE TABLE IF NOT EXISTS file_metadata (
    id UUID PRIMARY KEY,
    repository_id UUID NOT NULL REFERENCES code_repositories(id) ON DELETE CASCADE,
    file_path VARCHAR(700) NOT NULL,
    language VARCHAR(80),
    file_size_bytes BIGINT,
    chunk_count INTEGER NOT NULL DEFAULT 0,
    last_indexed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_file_metadata_repo_file_path ON file_metadata(repository_id, file_path);
CREATE INDEX IF NOT EXISTS idx_file_metadata_repository_id ON file_metadata(repository_id);
