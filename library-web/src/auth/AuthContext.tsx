import { createContext, useContext, useMemo, useState, type ReactNode } from 'react';
import type { AuthResponse } from '../types';
import { setStoredToken } from '../api/client';

const USER_KEY = 'elibrary.user';

export interface AuthUser {
  userId: string;
  email: string;
  name: string;
  role: string;
}

interface AuthContextValue {
  user: AuthUser | null;
  isAuthenticated: boolean;
  onAuthenticated: (auth: AuthResponse) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function readStoredUser(): AuthUser | null {
  try {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? (JSON.parse(raw) as AuthUser) : null;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(readStoredUser);

  const value = useMemo<AuthContextValue>(() => {
    const onAuthenticated = (auth: AuthResponse) => {
      const nextUser: AuthUser = {
        userId: auth.userId,
        email: auth.email,
        name: auth.name,
        role: auth.role,
      };
      setStoredToken(auth.token);
      try {
        localStorage.setItem(USER_KEY, JSON.stringify(nextUser));
      } catch {
        // ignore storage failures — session still lives in memory
      }
      setUser(nextUser);
    };

    const logout = () => {
      setStoredToken(null);
      try {
        localStorage.removeItem(USER_KEY);
      } catch {
        // ignore
      }
      setUser(null);
    };

    return { user, isAuthenticated: user !== null, onAuthenticated, logout };
  }, [user]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return ctx;
}
