import type { AuthResponse } from './types';

const BASE: string = import.meta.env.VITE_API_URL ?? '';
const KEY = 'cn_tokens';

interface Tokens { accessToken: string; refreshToken: string }
let tokens: Tokens | null = (() => {
  try { return JSON.parse(localStorage.getItem(KEY) || 'null'); } catch { return null; }
})();
let onSessionExpired: (() => void) | null = null;

export function setSessionExpiredHandler(fn: () => void) { onSessionExpired = fn; }
export function hasTokens() { return tokens !== null; }
export function saveSession(r: AuthResponse | null) {
  tokens = r ? { accessToken: r.accessToken, refreshToken: r.refreshToken } : null;
  if (tokens) localStorage.setItem(KEY, JSON.stringify(tokens)); else localStorage.removeItem(KEY);
}
export function refreshToken() { return tokens?.refreshToken ?? null; }

export class ApiError extends Error {
  status: number;
  constructor(status: number, message: string) { super(message); this.status = status; }
}

let refreshing: Promise<boolean> | null = null;
async function refresh(): Promise<boolean> {
  if (!tokens) return false;
  if (!refreshing) {
    const rt = tokens.refreshToken;
    refreshing = fetch(BASE + '/api/auth/refresh', {
      method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ refreshToken: rt })
    }).then(async res => {
      if (!res.ok) { saveSession(null); onSessionExpired?.(); return false; }
      saveSession(await res.json());
      return true;
    }).catch(() => false).finally(() => { refreshing = null; });
  }
  return refreshing;
}

interface Opts { method?: string; body?: unknown; blob?: boolean }

export async function api<T>(path: string, opts: Opts = {}, retry = true): Promise<T> {
  const headers: Record<string, string> = {};
  const isForm = opts.body instanceof FormData;
  if (opts.body !== undefined && !isForm) headers['Content-Type'] = 'application/json';
  if (tokens) headers.Authorization = 'Bearer ' + tokens.accessToken;
  const res = await fetch(BASE + path, {
    method: opts.method ?? (opts.body !== undefined ? 'POST' : 'GET'),
    headers,
    body: opts.body === undefined ? undefined : isForm ? (opts.body as FormData) : JSON.stringify(opts.body)
  });
  if (res.status === 401 && retry && tokens && !path.startsWith('/api/auth/')) {
    if (await refresh()) return api<T>(path, opts, false);
  }
  if (!res.ok) {
    let msg = res.status === 429 ? 'Too many requests. Please wait a minute.' : 'Something went wrong (' + res.status + ')';
    try { const j = await res.json(); msg = j.error || j.message || msg; } catch { /* not JSON */ }
    throw new ApiError(res.status, msg);
  }
  if (opts.blob) return (await res.blob()) as unknown as T;
  const text = await res.text();
  return (text ? JSON.parse(text) : null) as T;
}

/** Downloads an authenticated file (invoice, certificate). */
export async function download(path: string, filename: string) {
  const blob = await api<Blob>(path, { blob: true });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url; a.download = filename; document.body.appendChild(a); a.click(); a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 2000);
}

export const apiUrl = (path: string) => BASE + path;
