// src/pages/client/DeviceDetailsPage.tsx
import { useState } from "react";
import { useParams } from "react-router-dom";
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from "recharts";

interface DeviceSeriesPoint {
  x_virtual_hour: number;
  hour: number;
  ts_utc: string;
  minute: number;
  kwh: number;
}

export default function DeviceDetailsPage() {
  const { deviceId } = useParams<{ deviceId: string }>();
  const [date, setDate] = useState<string>("");
  const [series, setSeries] = useState<DeviceSeriesPoint[]>([]);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState("");

  const fetchSeries = async () => {
    if (!deviceId || !date) return;
    setLoading(true);
    setErr("");
    try {
      const url = `http://localhost/api/monitoring/devices/${deviceId}/series?date=${date}&tz=Europe/Bucharest&virtualHourMinutes=1`;
      const resp = await fetch(url);
      if (!resp.ok) throw new Error("Failed to fetch series");
      const data: DeviceSeriesPoint[] = await resp.json();
      setSeries(data);
    } catch (e: any) {
      setErr(e.message || "Error fetching data");
    } finally {
      setLoading(false);
    }
  };

  // Prepare data for graph
  const graphData = series.map(pt => ({
    hour: pt.hour,
    minute: pt.minute,
    time: `${pt.hour.toString().padStart(2, "0")}:${pt.minute.toString().padStart(2, "0")}`,
    kwh: pt.kwh
  }));

  return (
    <div style={{ padding: 24 }}>
      <h2>Device Details (ID: {deviceId})</h2>
      <div style={{ marginBottom: 16 }}>
        <label>
          Select date:
          <input type="date" value={date} onChange={e => setDate(e.target.value)} style={{ marginLeft: 8 }} />
        </label>
        <button onClick={fetchSeries} disabled={!date || loading} style={{ marginLeft: 12, padding: "6px 16px", borderRadius: 6, background: "#007bff", color: "#fff", border: "none", cursor: "pointer" }}>Load</button>
      </div>
      {err && <div style={{ color: "crimson", marginBottom: 12 }}>{err}</div>}
      {loading && <div>Loading...</div>}
      {series.length > 0 && (
        <div style={{ marginTop: 24 }}>
          <h3>kWh by Hour</h3>
          <ResponsiveContainer width="100%" height={350}>
            <LineChart data={graphData} margin={{ top: 20, right: 30, left: 20, bottom: 20 }}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="time" label={{ value: "Hour:Minute", position: "insideBottom", offset: -5 }} />
              <YAxis label={{ value: "kWh", angle: -90, position: "insideLeft" }} domain={[0, 'auto']} />
              <Tooltip />
              <Legend />
              <Line type="monotone" dataKey="kwh" stroke="#007bff" dot={{ r: 4 }} name="kWh" />
            </LineChart>
          </ResponsiveContainer>
        </div>
      )}
      {series.length === 0 && !loading && !err && date && <div>No data for selected date.</div>}
    </div>
  );
}
