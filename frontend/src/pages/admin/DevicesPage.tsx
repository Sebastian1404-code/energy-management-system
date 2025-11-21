
import { useEffect, useState } from "react";
import { DevicesApi } from "../../api/devices";
import { UsersApi } from "../../api/users";
import type { User } from "../../api/users";
import type { Device } from "../../api/devices";
import { useNavigate } from "react-router-dom";

const navBtnStyle = {
  padding: "8px 20px",
  borderRadius: 8,
  background: "#6c63ff",
  color: "#fff",
  border: "none",
  fontWeight: 500,
  fontSize: 16,
  cursor: "pointer",
  boxShadow: "0 2px 8px #6c63ff22"
};

export default function DevicesPage() {
    const [editId, setEditId] = useState<number | null>(null);
    const [editName, setEditName] = useState<string>("");
    const [editMax, setEditMax] = useState<number>(0);
  const [rows, setRows] = useState<Device[]>([]);
  const [users, setUsers] = useState<User[]>([]);
  const [name, setName] = useState<string>("");
  const [max, setMax] = useState<number>(0);
  const [userId, setUserId] = useState<number | "">("");
  const [err, setErr] = useState<string>("");
  const nav = useNavigate();

  const load = async () => {
    setRows(await DevicesApi.list());
    setUsers(await UsersApi.list());
  };
  useEffect(()=>{ load(); }, []);

  const handleCreate = async () => {
    if (!name || !max) { setErr("Device name and max value required"); return; }
    try {
      await DevicesApi.create({ name, maximConsumptionValue: max, userId: userId === "" ? undefined : userId });
      setName(""); setMax(0); setUserId(""); setErr("");
      await load();
    } catch (e: any) {
      setErr(e?.response?.data?.message ?? "Error creating device");
    }
  };

  return (
    <div style={{ padding: 24, maxWidth: 600, margin: "0 auto" }}>
      <h2 style={{ textAlign: "center", marginBottom: 24 }}>Device Management</h2>
      <div style={{ display: "flex", gap: 12, justifyContent: "center", marginBottom: 24 }}>
        <button onClick={()=>nav("/admin/users")} style={navBtnStyle}>Go to Users</button>
        <button onClick={()=>nav("/admin/assignments")} style={navBtnStyle}>Go to Assignments</button>
      </div>
      <div style={{ background: "#f8f9fa", borderRadius: 12, boxShadow: "0 2px 8px #0001", padding: 24, marginBottom: 32 }}>
        <h3 style={{ marginBottom: 16 }}>Add New Device</h3>
        {err && <div style={{ color: "crimson", marginBottom: 8 }}>{err}</div>}
        <div style={{ display: "grid", gridTemplateColumns: "2fr 1fr 1fr 1fr auto", gap: 12 }}>
          <input placeholder="Device name" value={name} onChange={e=>setName(e.target.value)} style={{ padding: 8, borderRadius: 6, border: "1px solid #ccc" }} />
          <input placeholder="Max consumption (W)" type="number" value={max} onChange={e=>setMax(Number(e.target.value)||0)} style={{ padding: 8, borderRadius: 6, border: "1px solid #ccc" }} />
          <select value={userId} onChange={e=>setUserId(e.target.value === "" ? "" : Number(e.target.value))} style={{ padding: 8, borderRadius: 6, border: "1px solid #ccc" }}>
            <option value="">No user</option>
            {users.map((u: User) => <option key={u.id} value={u.id}>{u.username} (#{u.id})</option>)}
          </select>
          <button onClick={handleCreate} style={{ padding: "8px 16px", borderRadius: 6, background: "#007bff", color: "#fff", border: "none", cursor: "pointer" }}>Create</button>
        </div>
      </div>
      <div style={{ background: "#fff", borderRadius: 12, boxShadow: "0 2px 8px #0001", padding: 24 }}>
        <h3 style={{ marginBottom: 16 }}>Device List</h3>
        <table style={{ width: "100%", borderCollapse: "collapse" }}>
          <thead>
            <tr style={{ background: "#f1f3f4" }}>
              <th style={{ padding: 8, borderBottom: "1px solid #eee" }}>ID</th>
              <th style={{ padding: 8, borderBottom: "1px solid #eee" }}>Name</th>
              <th style={{ padding: 8, borderBottom: "1px solid #eee" }}>Max Consumption (W)</th>
              <th style={{ padding: 8, borderBottom: "1px solid #eee" }}>User ID</th>
              <th style={{ padding: 8, borderBottom: "1px solid #eee" }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((d: Device) => (
              <tr key={d.id} style={{ borderBottom: "1px solid #eee" }}>
                <td style={{ padding: 8 }}>{d.id}</td>
                <td style={{ padding: 8 }}>{d.name}</td>
                <td style={{ padding: 8 }}>{d.maximConsumptionValue}W</td>
                <td style={{ padding: 8 }}>{d.userId ?? "-"}</td>
                <td style={{ padding: 8 }}>
                  <button onClick={() => {
                    setEditId(d.id);
                    setEditName(d.name);
                    setEditMax(d.maximConsumptionValue);
                  }} style={{ marginRight: 8, padding: "4px 12px", borderRadius: 6, background: "#ffc107", color: "#333", border: "none", cursor: "pointer" }}>Edit</button>
                  <button onClick={async ()=>{ await DevicesApi.remove(d.id!); await load(); }} style={{ padding: "4px 12px", borderRadius: 6, background: "#dc3545", color: "#fff", border: "none", cursor: "pointer" }}>Delete</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {editId !== null && (
          <div style={{ marginTop: 24, background: "#f8f9fa", borderRadius: 12, boxShadow: "0 2px 8px #0001", padding: 24 }}>
            <h3>Edit Device</h3>
            <input placeholder="Device name" value={editName} onChange={e=>setEditName(e.target.value)} style={{ padding: 8, borderRadius: 6, border: "1px solid #ccc", marginRight: 8 }} />
            <input placeholder="Max consumption (W)" type="number" value={editMax} onChange={e=>setEditMax(Number(e.target.value)||0)} style={{ padding: 8, borderRadius: 6, border: "1px solid #ccc", marginRight: 8 }} />
            <button onClick={async () => {
              await DevicesApi.updateMeta(editId, { name: editName, maximConsumptionValue: editMax });
              setEditId(null); setEditName(""); setEditMax(0); await load();
            }} style={{ padding: "8px 16px", borderRadius: 6, background: "#007bff", color: "#fff", border: "none", cursor: "pointer" }}>Save</button>
            <button onClick={() => { setEditId(null); setEditName(""); setEditMax(0); }} style={{ padding: "8px 16px", borderRadius: 6, background: "#dc3545", color: "#fff", border: "none", cursor: "pointer", marginLeft: 8 }}>Cancel</button>
          </div>
        )}
      </div>
    </div>
  );
}
