/**
 * Normalizes a repository URL by trimming whitespace, trailing slash, and .git suffix.
 */
export function normalizeRepoUrl(url: string): string {
  if (!url) return '';
  return url.trim().replace(/\/$/, '').replace(/\.git$/i, '');
}

/**
 * Extracts a concise display name for a repository URL (e.g. owner/repo).
 */
export function getRepoShortName(url: string): string {
  const normalized = normalizeRepoUrl(url);
  return normalized.replace(/^https?:\/\/github\.com\//i, '');
}
