// src/pages/Login.tsx
import { useState, useEffect } from "react";
import { useAuth } from "../auth/AuthContext";
import { loginApi } from "../api/auth";
import { useNavigate } from "react-router-dom";

export default function Login() {
  const [username, setU] = useState("");
  const [password, setP] = useState("");
  const [err, setErr] = useState(""); 
  const [pendingLogin, setPendingLogin] = useState(false);
  const { login, role } = useAuth(); 
  const nav = useNavigate();

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const { token } = await loginApi(username, password);
      login(token);
      setPendingLogin(true);
    } catch (e: any) {
      setErr(e?.response?.data?.message ?? "Login failed");
    }
  };

  useEffect(() => {
    if (!pendingLogin) return;
    if (role === "ADMIN") nav("/admin/users");
    else if (role === "CLIENT") nav("/me/devices");
    else if (role) nav("/");
    if (role) setPendingLogin(false);
  }, [role, nav, pendingLogin]);

  return (
    <div style={{ minHeight: "100vh", display: "flex", alignItems: "center", justifyContent: "center", background: "linear-gradient(120deg, #007bff 0%, #6c63ff 100%)" }}>
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
        <h2 style={{ textAlign: "center", color: "#007bff", marginBottom: 8, letterSpacing: 1 }}>Sign In</h2>
        {err && <div style={{ color: "crimson", textAlign: "center", fontWeight: 500 }}>{err}</div>}
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
        <button
          type="submit"
          style={{
            padding: "12px 0",
            borderRadius: 8,
            background: "linear-gradient(90deg, #007bff 0%, #6c63ff 100%)",
            color: "#fff",
            fontWeight: 600,
            fontSize: 17,
            border: "none",
            cursor: "pointer",
            boxShadow: "0 2px 8px #007bff33"
          }}
        >
          Login
        </button>
        <button
          type="button"
          onClick={() => nav("/register")}
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
          Don't have an account? Register
        </button>
      </form>
    </div>
  );
}
