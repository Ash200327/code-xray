import { ApiResponse, RepositoryView } from '../types';
import { apiFetch } from './http';

export async function listRepositories(): Promise<RepositoryView[]> {
  const res = await apiFetch('/api/repositories');
  const body: ApiResponse<RepositoryView[]> = await res.json();
  if (!res.ok || !body.success) {
    throw new Error(body.error?.message || `Server error: ${res.status}`);
  }
  return body.data;
}
