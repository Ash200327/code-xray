import { AuthResponse } from '../types';

const ACCESS_KEY = 'ca_access_token';
const REFRESH_KEY = 'ca_refresh_token';

export function getAccessToken(): string | null {
  return localStorage.getItem(ACCESS_KEY);
}

export function setTokens(accessToken: string, refreshToken: string): void {
  localStorage.setItem(ACCESS_KEY, accessToken);
  localStorage.setItem(REFRESH_KEY, refreshToken);
}

export function clearTokens(): void {
  localStorage.removeItem(ACCESS_KEY);
  localStorage.removeItem(REFRESH_KEY);
}

export const BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

export async function apiFetch(input: string, init: RequestInit = {}, retry = true): Promise<Response> {
  const token = getAccessToken();
  const headers = new Headers(init.headers || {});
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }
  if (init.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  const response = await fetch(`${BASE_URL}${input}`, { ...init, headers });
  if (response.status !== 401 || !retry) {
    return response;
  }

  const refreshed = await tryRefresh();
  if (!refreshed) {
    clearTokens();
    return response;
  }
  return apiFetch(input, init, false);
}

async function tryRefresh(): Promise<boolean> {
  const refreshToken = localStorage.getItem(REFRESH_KEY);
  if (!refreshToken) return false;

  const res = await fetch(`${BASE_URL}/api/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  });
  if (!res.ok) return false;

  const body = await res.json().catch(() => null) as { success?: boolean; data?: AuthResponse } | null;
  if (!body?.success || !body.data) return false;
  setTokens(body.data.accessToken, body.data.refreshToken);
  return true;
}

