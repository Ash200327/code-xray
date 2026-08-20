-- Drop old restrictive unique constraint on code_repositories
DROP INDEX IF EXISTS uq_code_repositories_repo_url_branch;

-- Allow multiple users to register and index the same repository independently
CREATE UNIQUE INDEX IF NOT EXISTS uq_code_repositories_user_repo_branch
    ON code_repositories(user_id, repo_url, branch)
    WHERE user_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_code_repositories_anon_repo_branch
    ON code_repositories(repo_url, branch)
    WHERE user_id IS NULL;

-- Add GIN index for fast JSONB metadata lookups in hybrid search & repo insights
CREATE INDEX IF NOT EXISTS idx_vector_store_metadata_gin
    ON vector_store USING gin (metadata jsonb_path_ops);
