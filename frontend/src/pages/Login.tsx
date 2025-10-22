// src/pages/Login.tsx
import { useState } from "react";
import { useAuth } from "../auth/AuthContext";
import { loginApi } from "../api/auth";
import { useNavigate } from "react-router-dom";

export default function Login() {
  const [username, setU] = useState("");
  const [password, setP] = useState("");
  const [err, setErr] = useState(""); 
  const { login } = useAuth(); 
  const nav = useNavigate();

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const { token } = await loginApi(username, password);
      login(token);
      nav("/"); // go to dashboard
    } catch (e: any) {
      setErr(e?.response?.data?.message ?? "Login failed");
    }
  };

  return (
    <form onSubmit={submit} style={{ maxWidth: 360, margin: "64px auto", display: "grid", gap: 12 }}>
      <h2>Sign in</h2>
      {err && <div style={{ color: "crimson" }}>{err}</div>}
      <input placeholder="Username" value={username} onChange={e=>setU(e.target.value)} />
      <input placeholder="Password" type="password" value={password} onChange={e=>setP(e.target.value)} />
      <button type="submit">Login</button>
    </form>
  );
}
