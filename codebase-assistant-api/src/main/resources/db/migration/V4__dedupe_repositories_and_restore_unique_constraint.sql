WITH ranked_repositories AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY repo_url, branch
               ORDER BY updated_at DESC NULLS LAST, created_at DESC NULLS LAST, id DESC
           ) AS rank_num
    FROM code_repositories
)
DELETE FROM code_repositories
WHERE id IN (
    SELECT id
    FROM ranked_repositories
    WHERE rank_num > 1
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_code_repositories_repo_url_branch
    ON code_repositories(repo_url, branch);
