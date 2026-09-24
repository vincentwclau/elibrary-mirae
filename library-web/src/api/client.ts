import axios, { AxiosError } from 'axios';
import type { ApiError } from '../types';

const TOKEN_KEY = 'elibrary.token';

export function getStoredToken(): string | null {
  try {
    return localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

export function setStoredToken(token: string | null): void {
  try {
    if (token) {
      localStorage.setItem(TOKEN_KEY, token);
    } else {
      localStorage.removeItem(TOKEN_KEY);
    }
  } catch {
    // localStorage unavailable (private mode etc.) — the in-memory session still works.
  }
}

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api',
  headers: { 'Content-Type': 'application/json' },
});

// Attach the bearer token to every request when present.
api.interceptors.request.use((config) => {
  const token = getStoredToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// A 401 means the token is missing/expired: clear it and bounce to login.
// Other errors are surfaced to the caller (React Query) as-is.
api.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiError>) => {
    if (error.response?.status === 401 && getStoredToken()) {
      setStoredToken(null);
      if (window.location.pathname !== '/login') {
        window.location.assign('/login');
      }
    }
    return Promise.reject(error);
  },
);

/** Extracts a human-readable message from an Axios error / ApiError body. */
export function toErrorMessage(error: unknown, fallback = 'Something went wrong'): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as ApiError | undefined;
    if (data?.fieldErrors?.length) {
      return data.fieldErrors.map((f) => `${f.field}: ${f.message}`).join(', ');
    }
    return data?.message ?? error.message ?? fallback;
  }
  return fallback;
}
