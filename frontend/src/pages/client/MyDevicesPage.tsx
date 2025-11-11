// src/pages/client/MyDevicesPage.tsx
import { useEffect, useState } from "react";
import { DevicesApi } from "../../api/devices";
import type { Device } from "../../api/devices";
import { useAuth } from "../../auth/AuthContext";
import { UsersApi } from "../../api/users";

export default function MyDevicesPage(){
  const [rows, setRows] = useState<Device[]>([]);
  const { username } = useAuth();

  useEffect(() => {
    (async () => {
      if (!username) return;
  // Get userId by username
  const userId = await UsersApi.getById(username);
  // Get all devices and filter by userId
  const allDevices = await DevicesApi.list();
  setRows(allDevices.filter(d => d.userId === userId));
    })();
  }, [username]);

  return (
    <div style={{ padding: 24 }}>
      <h2>My Devices {username ? `for ${username}` : ""}</h2>
      <ul>
        {rows.map(d => <li key={d.id}>{d.name} — {d.maximConsumptionValue}W</li>)}
      </ul>
    </div>
  );
}
