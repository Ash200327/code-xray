-- Drop old restrictive unique constraint on code_repositories
DROP INDEX IF EXISTS uq_code_repositories_repo_url_branch;

-- Allow multiple users to register and index the same repository independently
CREATE UNIQUE INDEX IF NOT EXISTS uq_code_repositories_user_repo_branch
    ON code_repositories(user_id, repo_url, branch)
    WHERE user_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_code_repositories_anon_repo_branch
    ON code_repositories(repo_url, branch)
    WHERE user_id IS NULL;

-- Ensure metadata is jsonb so GIN indexing and JSON operators are fully supported
ALTER TABLE vector_store ALTER COLUMN metadata TYPE jsonb USING metadata::jsonb;

-- Add GIN index for fast JSONB metadata lookups in hybrid search & repo insights
CREATE INDEX IF NOT EXISTS idx_vector_store_metadata_gin
    ON vector_store USING gin (metadata jsonb_path_ops);

-- Add index on repo_url expression for fast metadata filter operations
CREATE INDEX IF NOT EXISTS idx_vector_store_repo_url
    ON vector_store ((metadata->>'repo_url'));
