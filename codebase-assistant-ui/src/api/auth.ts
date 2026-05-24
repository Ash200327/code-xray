import { ApiResponse, AuthResponse, LoginRequest, SignUpRequest, UserView } from '../types';
import { apiFetch, clearTokens, setTokens, BASE_URL } from './http';

export async function signup(payload: SignUpRequest): Promise<AuthResponse> {
  return authenticate('/api/auth/signup', payload);
}

export async function login(payload: LoginRequest): Promise<AuthResponse> {
  return authenticate('/api/auth/login', payload);
}

export async function me(): Promise<UserView> {
  const res = await apiFetch('/api/auth/me');
  const body: ApiResponse<UserView> = await res.json();
  if (!res.ok || !body.success) {
    throw new Error(body.error?.message || `Server error: ${res.status}`);
  }
  return body.data;
}

export function logout(): void {
  clearTokens();
}

async function authenticate(url: string, payload: object): Promise<AuthResponse> {
  const res = await fetch(`${BASE_URL}${url}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  const body: ApiResponse<AuthResponse> = await res.json();
  if (!res.ok || !body.success) {
    throw new Error(body.error?.message || `Server error: ${res.status}`);
  }
  setTokens(body.data.accessToken, body.data.refreshToken);
  return body.data;
}

