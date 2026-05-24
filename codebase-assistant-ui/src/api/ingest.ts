import { ApiResponse, IngestResultData, IngestionJobData } from '../types';
import { apiFetch } from './http';

export async function ingestRepo(repoUrl: string, branch = 'main'): Promise<IngestResultData> {
  const res = await apiFetch('/api/ingest', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ repoUrl, branch }),
  });

  if (!res.ok) {
    const data = await res.json().catch(() => ({}));
    throw new Error(data?.status || `Server error: ${res.status}`);
  }

  return res.json();
}

export async function startIngestionJob(repoUrl: string, branch = 'main'): Promise<IngestionJobData> {
  const res = await apiFetch('/api/ingestion/jobs', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ repoUrl, branch }),
  });

  const data: ApiResponse<IngestionJobData> = await res.json().catch(() => ({ success: false, data: {} as IngestionJobData }));
  if (!res.ok || !data.success) {
    throw new Error(data?.error?.message || `Server error: ${res.status}`);
  }
  return data.data;
}

export async function getIngestionJob(jobId: string): Promise<IngestionJobData> {
  const res = await apiFetch(`/api/ingestion/jobs/${jobId}`);
  const data: ApiResponse<IngestionJobData> = await res.json().catch(() => ({ success: false, data: {} as IngestionJobData }));
  if (!res.ok || !data.success) {
    throw new Error(data?.error?.message || `Server error: ${res.status}`);
  }
  return data.data;
}
