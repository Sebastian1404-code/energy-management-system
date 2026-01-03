import { useEffect, useRef, useState } from "react";
import { DevicesApi } from "../../api/devices";
import type { Device } from "../../api/devices";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { useAuth } from "../../auth/AuthContext";

type OverconsumptionAlertDto = {
  userId: number;
  deviceId: number;
  windowStartUtc: string;
  windowMinutes: number;
  kwhSoFar: number;
  maxConsumptionValue: number;
  createdAtUtc: string;
};

const LOCALSTORAGE_KEY = (userId: string) => `alerts_${userId}`;

export default function AlertsPanel() {
  const { userId } = useAuth();
  const [alerts, setAlerts] = useState<OverconsumptionAlertDto[]>([]);
  const [unread, setUnread] = useState(0);
  const [deviceNames, setDeviceNames] = useState<Record<number, string>>({});
  const clientRef = useRef<Client | null>(null);

  // Fetch device name using DevicesApi for a given deviceId if not already loaded
  const fetchDeviceName = async (deviceId: number) => {
    if (deviceNames[deviceId]) return;
    try {
      const device: Device = await DevicesApi.getById(deviceId);
      setDeviceNames(prev => ({ ...prev, [deviceId]: device.name || `Device ${deviceId}` }));
    } catch {
      setDeviceNames(prev => ({ ...prev, [deviceId]: `Device ${deviceId}` }));
    }
  };

  // Load alerts from localStorage
  useEffect(() => {
    if (!userId) return;
    const saved = localStorage.getItem(LOCALSTORAGE_KEY(userId));
    if (saved) setAlerts(JSON.parse(saved));
  }, [userId]);

  // Connect to STOMP WebSocket
  useEffect(() => {
    if (!userId) return;
    const sock = new SockJS(`/ws?userId=${userId}&role=USER`);
    const client = new Client({
      webSocketFactory: () => sock,
      reconnectDelay: 5000,
      debug: () => {},
    });

    client.onConnect = () => {
      client.subscribe(`/topic/alerts.user.${userId}`, (msg) => {
        try {
          const alert: OverconsumptionAlertDto = JSON.parse(msg.body);
          setAlerts(prev => {
            const updated = [alert, ...prev];
            localStorage.setItem(LOCALSTORAGE_KEY(userId), JSON.stringify(updated));
            return updated;
          });
          setUnread(u => u + 1);
          fetchDeviceName(alert.deviceId);
        } catch {}
      });
    };

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
    };
  }, [userId]);

  // Fetch device names for all alerts on load
  useEffect(() => {
    alerts.forEach(a => fetchDeviceName(a.deviceId));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [alerts]);

  // Mark all as read when panel is opened (panel is always open in this implementation)
  useEffect(() => {
    setUnread(0);
  }, []);

  const handleClear = () => {
    setAlerts([]);
    if (userId) localStorage.removeItem(LOCALSTORAGE_KEY(userId));
    setUnread(0);
  };

  const handleMarkAllRead = () => setUnread(0);

  return (
    <div style={{ maxWidth: 700, margin: "40px auto", background: "#fff", borderRadius: 16, boxShadow: "0 4px 24px #0002", padding: 24 }}>
      <div style={{ display: "flex", alignItems: "center", marginBottom: 16 }}>
        <h2 style={{ flex: 1, color: "#dc3545" }}>Overconsumption Alerts</h2>
        <span style={{
          background: unread > 0 ? "#dc3545" : "#ccc",
          color: "#fff",
          borderRadius: "50%",
          padding: "6px 12px",
          fontWeight: 700,
          fontSize: 16,
          marginRight: 12
        }}>{unread}</span>
        <button onClick={handleMarkAllRead} style={{ marginRight: 8 }}>Mark all read</button>
        <button onClick={handleClear}>Clear alerts</button>
      </div>
      <table style={{ width: "100%", borderCollapse: "collapse", background: "#f8f9fa", borderRadius: 8 }}>
        <thead>
          <tr style={{ background: "#e9eafc" }}>
            <th style={{ padding: 8 }}>Time</th>
            <th style={{ padding: 8 }}>Device</th>
            <th style={{ padding: 8 }}>kWh</th>
            <th style={{ padding: 8 }}>Threshold</th>
            <th style={{ padding: 8 }}>Window Start</th>
            <th style={{ padding: 8 }}>Window Minutes</th>
          </tr>
        </thead>
        <tbody>
          {alerts.length === 0 && (
            <tr>
              <td colSpan={6} style={{ textAlign: "center", color: "#888", padding: 24 }}>No alerts.</td>
            </tr>
          )}
          {alerts.map((a, i) => (
            <tr key={i}>
              <td style={{ padding: 8 }}>{new Date(a.createdAtUtc).toLocaleString()}</td>
              <td style={{ padding: 8 }}>
                {deviceNames[a.deviceId] ? `${deviceNames[a.deviceId]} (#${a.deviceId})` : a.deviceId}
              </td>
              <td style={{ padding: 8 }}>{a.kwhSoFar}</td>
              <td style={{ padding: 8 }}>{a.maxConsumptionValue}</td>
              <td style={{ padding: 8 }}>{new Date(a.windowStartUtc).toLocaleString()}</td>
              <td style={{ padding: 8 }}>{a.windowMinutes}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
