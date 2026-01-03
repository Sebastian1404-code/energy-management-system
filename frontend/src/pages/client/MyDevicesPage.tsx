// src/pages/client/MyDevicesPage.tsx
import { useEffect, useState } from "react";
import { DevicesApi } from "../../api/devices";
import { getDeviceSummaries } from "../../api/monitoring";
import type { Device } from "../../api/devices";
import { useAuth } from "../../auth/AuthContext";
import { useNavigate } from "react-router-dom";
import { UsersApi } from "../../api/users";

// Type for device summary from monitoring endpoint
export interface DeviceSummary {
  device_id: string;
  latest_window_start_utc: string;
  window_minutes: number;
  kwh_in_latest_window: number;
  sample_count_in_latest_window: number;
  last_event_utc: string;
}

const cardStyle = {
  background: "#fff",
  borderRadius: 16,
  boxShadow: "0 4px 24px #0002",
  padding: 24,
  marginBottom: 24,
  maxWidth: 500,
  margin: "0 auto"
};

const deviceBtnStyle = {
  display: "block",
  width: "100%",
  textAlign: "left" as const,
  background: "linear-gradient(90deg, #6c63ff 0%, #007bff 100%)",
  color: "#fff",
  border: "none",
  borderRadius: 12,
  padding: "18px 24px",
  marginBottom: 18,
  fontSize: 18,
  fontWeight: 600,
  cursor: "pointer",
  boxShadow: "0 2px 8px #007bff22",
  transition: "background 0.2s, box-shadow 0.2s"
};

const detailsStyle = {
  marginTop: 10,
  fontSize: 15,
  color: "#444",
  background: "#f8f9fa",
  borderRadius: 10,
  padding: "10px 16px"
};

export default function MyDevicesPage() {
  const [rows, setRows] = useState<Device[]>([]);
  const [summaries, setSummaries] = useState<DeviceSummary[]>([]);
  const { username } = useAuth();
  const nav = useNavigate();

  useEffect(() => {
    (async () => {
      if (!username) return;
      // Get userId by username
      const userId = await UsersApi.getById(username);
      // Get all devices and filter by userId
      const allDevices = await DevicesApi.list();
      setRows(allDevices.filter(d => d.userId === userId));
      // Fetch device summaries from monitoring endpoint
      const data = await getDeviceSummaries();
      setSummaries(Array.isArray(data) ? data : []);
    })();
  }, [username]);

  // Helper to match summary by device id
  function getSummary(device: Device) {
    const paddedId = device.id.toString().padStart(3, "0");
    return summaries.find(s => s.device_id === `device-${paddedId}`);
  }

  return (
    <div style={{ minHeight: "100vh", background: "linear-gradient(120deg, #e9eafc 0%, #f8f9fa 100%)", padding: "40px 0" }}>
      <div style={cardStyle}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 16 }}>
          <h2 style={{ textAlign: "center", color: "#6c63ff", marginBottom: 0, letterSpacing: 1 }}>
            My Devices {username ? `for ${username}` : ""}
          </h2>
          <button onClick={() => nav("/alerts")}
            style={{ padding: "8px 16px", borderRadius: 8, background: "#dc3545", color: "#fff", border: "none", fontWeight: 500, fontSize: 16, cursor: "pointer", marginLeft: 16 }}>
            View Alerts
          </button>
        </div>
        {rows.length === 0 && (
          <div style={{ textAlign: "center", color: "#888", fontSize: 17, marginBottom: 24 }}>
            No devices assigned to your account.
          </div>
        )}
        {rows.map(d => {
          const summary = getSummary(d);
          return (
            <button
              key={d.id}
              style={deviceBtnStyle}
              onClick={() => nav(`/client/device/${d.id}`)}
              onMouseOver={e => (e.currentTarget.style.boxShadow = "0 4px 16px #007bff44")}
              onMouseOut={e => (e.currentTarget.style.boxShadow = deviceBtnStyle.boxShadow!)}
            >
              <div style={{ fontSize: 20, fontWeight: 700, marginBottom: 4 }}>{d.name}</div>
              <div style={{ fontSize: 16, color: "#e9eafc", marginBottom: 6 }}>Max Consumption: {d.maximConsumptionValue}W</div>
              {summary && (
                <div style={detailsStyle}>
                  <div><b>Latest Window Start (UTC):</b> {summary.latest_window_start_utc}</div>
                  <div><b>Window Minutes:</b> {summary.window_minutes}</div>
                  <div><b>kWh:</b> {summary.kwh_in_latest_window}</div>
                  <div><b>Sample Count:</b> {summary.sample_count_in_latest_window}</div>
                </div>
              )}
            </button>
          );
        })}
      </div>
    </div>
  );
}
