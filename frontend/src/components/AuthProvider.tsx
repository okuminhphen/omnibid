"use client";

import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode
} from "react";
import {
  bootstrapAuthSession,
  getAuthState,
  logoutAuthSession,
  subscribeAuth
} from "@/services/authSession";
import type { AuthState } from "@/types/auth";

interface AuthContextValue extends AuthState {
  isAuthenticated: boolean;
  isAdmin: boolean;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>(getAuthState());

  useEffect(() => subscribeAuth(setState), []);
  useEffect(() => {
    void bootstrapAuthSession();
  }, []);

  const value = useMemo<AuthContextValue>(() => ({
    ...state,
    isAuthenticated: Boolean(state.accessToken && state.user),
    isAdmin: state.user?.roles.includes("ADMIN") ?? false,
    logout: logoutAuthSession
  }), [state]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used inside AuthProvider");
  return context;
}
