// src/auth/RequireAuth.tsx
import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "./AuthContext";

export default function RequireAuth() {
  const { token } = useAuth();
  return token ? <Outlet/> : <Navigate to="/login" replace />;
}
