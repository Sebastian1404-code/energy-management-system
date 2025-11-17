// src/pages/admin/AssignmentsPage.tsx

import { useEffect, useState } from "react";
import { UsersApi } from "../../api/users";
import type { User } from "../../api/users";
import { DevicesApi } from "../../api/devices";
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

export default function AssignmentsPage(){
  const [devices, setDevices] = useState<Device[]>([]);
  const [users, setUsers] = useState<User[]>([]);
  const [selDevice, setSelDevice] = useState<number>();
  const [selUser, setSelUser] = useState<number>();
  const [err, setErr] = useState("");
  const [success, setSuccess] = useState("");
  const nav = useNavigate();

  const load = async () => { setDevices(await DevicesApi.list()); setUsers(await UsersApi.list()); }
  useEffect(()=>{ load(); }, []);

  const handleAssign = async () => {
    if (!selDevice || !selUser) { setErr("Select both device and user"); return; }
    try {
      await DevicesApi.assignToUser(selDevice!, selUser!);
      setSuccess("Device assigned successfully!");
      setErr("");
      setSelDevice(undefined);
      setSelUser(undefined);
      await load();
      setTimeout(()=>setSuccess(""), 2000);
    } catch (e: any) {
      setErr(e?.response?.data?.message ?? "Error assigning device");
      setSuccess("");
    }
  };

  return (
    <div style={{ padding: 24, maxWidth: 500, margin: "0 auto" }}>
      <h2 style={{ textAlign: "center", marginBottom: 24 }}>Assign Device to User</h2>
      <div style={{ display: "flex", gap: 12, justifyContent: "center", marginBottom: 24 }}>
        <button onClick={()=>nav("/admin/users")} style={navBtnStyle}>Go to Users</button>
        <button onClick={()=>nav("/admin/devices")} style={navBtnStyle}>Go to Devices</button>
      </div>
      <div style={{ background: "#f8f9fa", borderRadius: 12, boxShadow: "0 2px 8px #0001", padding: 24 }}>
        <h3 style={{ marginBottom: 16 }}>Assignment</h3>
        {err && <div style={{ color: "crimson", marginBottom: 8 }}>{err}</div>}
        {success && <div style={{ color: "#28a745", marginBottom: 8 }}>{success}</div>}
        <div style={{ display: "grid", gridTemplateColumns: "2fr 2fr auto", gap: 12 }}>
          <select value={selDevice ?? ""} onChange={e=>setSelDevice(parseInt(e.target.value))} style={{ padding: 8, borderRadius: 6, border: "1px solid #ccc" }}>
            <option value="">Select device</option>
            {devices.map(d=> <option key={d.id} value={d.id}>{d.name} (#{d.id})</option>)}
          </select>
          <select value={selUser ?? ""} onChange={e=>setSelUser(parseInt(e.target.value))} style={{ padding: 8, borderRadius: 6, border: "1px solid #ccc" }}>
            <option value="">Select user</option>
            {users.map(u=> <option key={u.id} value={u.id}>{u.username} (#{u.id})</option>)}
          </select>
          <button disabled={!selDevice || !selUser} onClick={handleAssign} style={{ padding: "8px 16px", borderRadius: 6, background: "#007bff", color: "#fff", border: "none", cursor: "pointer" }}>Assign</button>
        </div>
      </div>
    </div>
  );
}
