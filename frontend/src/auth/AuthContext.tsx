// src/auth/AuthContext.tsx
import React, { createContext, useContext, useMemo, useState } from "react";
import {jwtDecode} from "jwt-decode";

type Role = "ADMIN" | "CLIENT";
type Decoded = { sub: string; roles?: Role[] | string[]; exp?: number };

type AuthState = {
  token: string | null;
  role: Role | null;
  username: string | null;
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

  // Extract a single role (string) from decoded.roles
  const role: Role | null = useMemo(() => {
    const r = decoded?.roles;
    return r ? (Array.isArray(r) ? r[0] as Role : r as Role) : null;
  }, [decoded]);

  const username = decoded?.sub ?? null;

  const login = (t: string) => { localStorage.setItem("token", t); setToken(t); };
  const logout = () => { localStorage.removeItem("token"); setToken(null); };

  return (
    <AuthCtx.Provider value={{ token, role, username, login, logout }}>
      {children}
    </AuthCtx.Provider>
  );
};

export const useAuth = () => useContext(AuthCtx);
