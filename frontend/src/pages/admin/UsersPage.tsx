// src/pages/admin/UsersPage.tsx
import { useEffect, useState } from "react";
import { UsersApi } from "../../api/users";
import type { User } from "../../api/users";

export default function UsersPage(){
  const [rows, setRows] = useState<User[]>([]);
  const [name, setName] = useState("");
  const [role, setRole] = useState<"ADMIN"|"CLIENT">("CLIENT");
  const [password, setPassword] = useState("");
  const [err, setErr] = useState("");

  const load = async () => setRows(await UsersApi.list());
  useEffect(() => { load(); }, []);

  const handleCreate = async () => {
    if (!name || !password) { setErr("Username and password required"); return; }
    try {
      await UsersApi.create({ username: name, role, password });
      setName(""); setPassword(""); setErr("");
      await load();
    } catch (e: any) {
      setErr(e?.response?.data?.message ?? "Error creating user");
    }
  };

  return (
    <div style={{ padding: 24, maxWidth: 600, margin: "0 auto" }}>
      <h2 style={{ textAlign: "center", marginBottom: 24 }}>User Management</h2>
      <div style={{ background: "#f8f9fa", borderRadius: 12, boxShadow: "0 2px 8px #0001", padding: 24, marginBottom: 32 }}>
        <h3 style={{ marginBottom: 16 }}>Create New User</h3>
        {err && <div style={{ color: "crimson", marginBottom: 8 }}>{err}</div>}
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr 1fr auto", gap: 12 }}>
          <input placeholder="Username" value={name} onChange={e=>setName(e.target.value)} style={{ padding: 8, borderRadius: 6, border: "1px solid #ccc" }} />
          <input placeholder="Password" type="password" value={password} onChange={e=>setPassword(e.target.value)} style={{ padding: 8, borderRadius: 6, border: "1px solid #ccc" }} />
          <select value={role} onChange={e=>setRole(e.target.value as any)} style={{ padding: 8, borderRadius: 6, border: "1px solid #ccc" }}>
            <option value="CLIENT">CLIENT</option>
            <option value="ADMIN">ADMIN</option>
          </select>
          <button onClick={handleCreate} style={{ padding: "8px 16px", borderRadius: 6, background: "#007bff", color: "#fff", border: "none", cursor: "pointer" }}>Create</button>
        </div>
      </div>
      <div style={{ background: "#fff", borderRadius: 12, boxShadow: "0 2px 8px #0001", padding: 24 }}>
        <h3 style={{ marginBottom: 16 }}>User List</h3>
        <table style={{ width: "100%", borderCollapse: "collapse" }}>
          <thead>
            <tr style={{ background: "#f1f3f4" }}>
              <th style={{ padding: 8, borderBottom: "1px solid #eee" }}>ID</th>
              <th style={{ padding: 8, borderBottom: "1px solid #eee" }}>Username</th>
              <th style={{ padding: 8, borderBottom: "1px solid #eee" }}>Role</th>
              <th style={{ padding: 8, borderBottom: "1px solid #eee" }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {rows.map(u => (
              <tr key={u.id} style={{ borderBottom: "1px solid #eee" }}>
                <td style={{ padding: 8 }}>{u.id}</td>
                <td style={{ padding: 8 }}>{u.username}</td>
                <td style={{ padding: 8 }}>{u.role}</td>
                <td style={{ padding: 8 }}>
                  <button onClick={async ()=>{ await UsersApi.remove(u.id); await load(); }} style={{ padding: "4px 12px", borderRadius: 6, background: "#dc3545", color: "#fff", border: "none", cursor: "pointer" }}>Delete</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
