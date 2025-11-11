// src/pages/Dashboard.tsx
import { useAuth } from "../auth/AuthContext";
import { useNavigate } from "react-router-dom";

export default function Dashboard() {
  const { username, role, logout } = useAuth();
  const nav = useNavigate();

  return (
    <div style={{ minHeight: "100vh", display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", background: "linear-gradient(120deg, #6c63ff 0%, #007bff 100%)" }}>
      <button
        onClick={() => { logout(); nav("/login"); }}
        style={{ position: "absolute", top: 24, right: 24, padding: "8px 20px", borderRadius: 8, background: "#dc3545", color: "#fff", border: "none", fontWeight: 500, fontSize: 16, cursor: "pointer", boxShadow: "0 2px 8px #dc354522" }}
      >
        Log Out
      </button>
      <div style={{ background: "#fff", borderRadius: 16, boxShadow: "0 4px 24px #0002", padding: 40, minWidth: 340, textAlign: "center" }}>
        <h2 style={{ color: "#007bff", marginBottom: 12, letterSpacing: 1 }}>Welcome, {username}</h2>
        <div style={{ fontSize: 18, color: "#444", marginBottom: 8 }}>Your role:</div>
        <div style={{ fontWeight: 600, fontSize: 20, color: "#6c63ff", marginBottom: 24 }}>{role || "none"}</div>
        <div style={{ color: "#888", fontSize: 15 }}>Use the navigation bar above to access your pages.</div>
      </div>
    </div>
  );
}

