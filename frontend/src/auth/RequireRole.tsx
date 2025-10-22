// src/auth/RequireRole.tsx
import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "./AuthContext";

export default function RequireRole({ allowed }: { allowed: ("ADMIN"|"CLIENT")[] }) {
  const { role } = useAuth();
  const ok = role ? allowed.includes(role) : false;
  return ok ? <Outlet/> : <Navigate to="/" replace />;
}
