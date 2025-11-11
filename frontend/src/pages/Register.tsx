// src/pages/Register.tsx
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { registerApi } from "../api/auth";

export default function Register() {
  const [username, setU] = useState("");
  const [password, setP] = useState("");
  const [role, setRole] = useState<"CLIENT"|"ADMIN">("CLIENT");
  const [err, setErr] = useState("");
  const [success, setSuccess] = useState("");
  const nav = useNavigate();

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await registerApi(username, password, role);
      setSuccess("Registration successful! You can now log in.");
      setErr("");
      setTimeout(() => nav("/login"), 1200);
    } catch (e: any) {
      setErr(e?.response?.data?.message ?? "Registration failed");
      setSuccess("");
    }
  };

  return (
    <div style={{ minHeight: "100vh", display: "flex", alignItems: "center", justifyContent: "center", background: "linear-gradient(120deg, #6c63ff 0%, #007bff 100%)" }}>
      <form onSubmit={submit} style={{
        width: 360,
        background: "#fff",
        borderRadius: 16,
        boxShadow: "0 4px 24px #0002",
        padding: 32,
        display: "grid",
        gap: 18,
        alignItems: "center"
      }}>
        <h2 style={{ textAlign: "center", color: "#6c63ff", marginBottom: 8, letterSpacing: 1 }}>Register</h2>
        {err && <div style={{ color: "crimson", textAlign: "center", fontWeight: 500 }}>{err}</div>}
        {success && <div style={{ color: "#28a745", textAlign: "center", fontWeight: 500 }}>{success}</div>}
        <input
          placeholder="Username"
          value={username}
          onChange={e=>setU(e.target.value)}
          style={{ padding: 12, borderRadius: 8, border: "1px solid #ddd", fontSize: 16 }}
          autoFocus
        />
        <input
          placeholder="Password"
          type="password"
          value={password}
          onChange={e=>setP(e.target.value)}
          style={{ padding: 12, borderRadius: 8, border: "1px solid #ddd", fontSize: 16 }}
        />
        <select value={role} onChange={e=>setRole(e.target.value as any)} style={{ padding: 12, borderRadius: 8, border: "1px solid #ddd", fontSize: 16 }}>
          <option value="CLIENT">CLIENT</option>
          <option value="ADMIN">ADMIN</option>
        </select>
        <button
          type="submit"
          style={{
            padding: "12px 0",
            borderRadius: 8,
            background: "linear-gradient(90deg, #6c63ff 0%, #007bff 100%)",
            color: "#fff",
            fontWeight: 600,
            fontSize: 17,
            border: "none",
            cursor: "pointer",
            boxShadow: "0 2px 8px #6c63ff33"
          }}
        >
          Register
        </button>
        <button
          type="button"
          onClick={() => nav("/login")}
          style={{
            padding: "8px 0",
            borderRadius: 8,
            background: "#f8f9fa",
            color: "#007bff",
            fontWeight: 500,
            fontSize: 15,
            border: "none",
            cursor: "pointer",
            marginTop: 4
          }}
        >
          Already have an account? Login
        </button>
      </form>
    </div>
  );
}
