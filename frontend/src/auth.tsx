import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react';
import { api, hasTokens, refreshToken, saveSession, setSessionExpiredHandler } from './api';
import type { AuthResponse, User } from './types';

interface AuthCtx {
  user: User | null;
  ready: boolean;
  login: (r: AuthResponse) => void;
  logout: () => Promise<void>;
  setUser: (u: User) => void;
}

const Ctx = createContext<AuthCtx>(null as unknown as AuthCtx);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    setSessionExpiredHandler(() => setUser(null));
    if (!hasTokens()) { setReady(true); return; }
    api<User>('/api/me').then(setUser).catch(() => saveSession(null)).finally(() => setReady(true));
  }, []);

  const login = useCallback((r: AuthResponse) => { saveSession(r); setUser(r.user); }, []);
  const logout = useCallback(async () => {
    const rt = refreshToken();
    if (rt) await api('/api/auth/logout', { body: { refreshToken: rt } }).catch(() => undefined);
    saveSession(null);
    setUser(null);
  }, []);

  return <Ctx.Provider value={{ user, ready, login, logout, setUser }}>{children}</Ctx.Provider>;
}

export const useAuth = () => useContext(Ctx);
