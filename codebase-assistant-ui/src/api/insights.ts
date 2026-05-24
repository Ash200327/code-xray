import { ApiResponse, RepositoryDocsView, RepositorySummaryView } from '../types';
import { apiFetch } from './http';

export async function getRepositorySummary(repositoryId: string): Promise<RepositorySummaryView> {
  const res = await apiFetch(`/api/repositories/${repositoryId}/summary`);
  const body: ApiResponse<RepositorySummaryView> = await res.json();
  if (!res.ok || !body.success) {
    throw new Error(body.error?.message || `Server error: ${res.status}`);
  }
  return body.data;
}

export async function getRepositoryDocs(repositoryId: string): Promise<RepositoryDocsView> {
  const res = await apiFetch(`/api/repositories/${repositoryId}/docs`);
  const body: ApiResponse<RepositoryDocsView> = await res.json();
  if (!res.ok || !body.success) {
    throw new Error(body.error?.message || `Server error: ${res.status}`);
  }
  return body.data;
}

