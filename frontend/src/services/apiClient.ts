const API_BASE = '/api';

/**
 * Centralized API client for Vaultiq.
 * Automatically injects the JWT Bearer token from localStorage
 * into every outbound request.
 */
async function request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const token = localStorage.getItem('vaultiq_token');

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string> || {}),
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const res = await fetch(`${API_BASE}${endpoint}`, {
    ...options,
    headers,
  });

  if (res.status === 401) {
    // Token expired or invalid — flush and redirect
    localStorage.removeItem('vaultiq_token');
    window.location.href = '/login';
    throw new Error('Session expired');
  }

  const json = await res.json();

  if (!res.ok) {
    throw new Error(json.message || `API error: ${res.status}`);
  }

  // Backend wraps everything in ApiResponse<T> → { success, message, data }
  return json.data as T;
}

// ── Typed Shortcuts ──

export const api = {
  get: <T>(endpoint: string) => request<T>(endpoint, { method: 'GET' }),

  post: <T>(endpoint: string, body: unknown) =>
    request<T>(endpoint, { method: 'POST', body: JSON.stringify(body) }),

  put: <T>(endpoint: string, body: unknown) =>
    request<T>(endpoint, { method: 'PUT', body: JSON.stringify(body) }),

  patch: <T>(endpoint: string, body?: unknown) =>
    request<T>(endpoint, { method: 'PATCH', body: body ? JSON.stringify(body) : undefined }),

  delete: <T>(endpoint: string) => request<T>(endpoint, { method: 'DELETE' }),
};
