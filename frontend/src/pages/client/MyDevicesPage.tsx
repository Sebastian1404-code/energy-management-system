// src/pages/client/MyDevicesPage.tsx
import { useEffect, useState } from "react";
import { DevicesApi } from "../../api/devices";
import type { Device } from "../../api/devices";
import { useAuth } from "../../auth/AuthContext";

export default function MyDevicesPage(){
  const [rows, setRows] = useState<Device[]>([]);
  const { username } = useAuth();

  useEffect(()=>{ (async ()=>{
    // Back-end should expose /api/devices/me or /api/devices?userId=...
    // For now we filter client-side assuming API returns only my devices if role=CLIENT
    setRows(await DevicesApi.list());
  })() }, []);

  return (
    <div style={{ padding: 24 }}>
      <h2>My Devices {username ? `for ${username}` : ""}</h2>
      <ul>
        {rows.map(d => <li key={d.id}>{d.name} — {d.maximConsumptionValue}W</li>)}
      </ul>
    </div>
  );
}
