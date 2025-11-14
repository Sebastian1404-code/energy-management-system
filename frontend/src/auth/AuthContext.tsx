// src/auth/AuthContext.tsx
import React, { createContext, useContext, useMemo, useState } from "react";
import {jwtDecode} from "jwt-decode";

type Role = "ADMIN" | "CLIENT";
type Decoded = { sub: string; username?: string; role?: Role | string; exp?: number };

type AuthState = {
  token: string | null;
  role: Role | null;
  username: string | null;
  userId: string | null;
  login: (token: string) => void;
  logout: () => void;
};

const AuthCtx = createContext<AuthState>(null!);

export const AuthProvider: React.FC<{children: React.ReactNode}> = ({ children }) => {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem("token"));
  const decoded: Decoded | null = useMemo(() => {
    if (!token) return null;
    try { return jwtDecode(token) as Decoded; } catch { return null; }
  }, [token]);

  // Extract role from decoded.role
  const role: Role | null = useMemo(() => {
    const r = decoded?.role;
    return r ? (r as Role) : null;
  }, [decoded]);

  const username = decoded?.username ?? null;
  const userId = decoded?.sub ?? null;

  const login = (t: string) => { localStorage.setItem("token", t); setToken(t); };
  const logout = () => { localStorage.removeItem("token"); setToken(null); };

  return (
    <AuthCtx.Provider value={{ token, role, username, userId, login, logout }}>
      {children}
    </AuthCtx.Provider>
  );
};

export const useAuth = () => useContext(AuthCtx);
