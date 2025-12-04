// src/App.tsx
import { BrowserRouter, Routes, Route, Navigate, useNavigate } from "react-router-dom";
import { AuthProvider, useAuth } from "./auth/AuthContext";
import RequireAuth from "./auth/RequireAuth";
import RequireRole from "./auth/RequireRole";
import Login from "./pages/Login";
import Dashboard from "./pages/Dashboard";
import UsersPage from "./pages/admin/UsersPage";
import DevicesPage from "./pages/admin/DevicesPage";
import AssignmentsPage from "./pages/admin/AssignmentsPage";
import MyDevicesPage from "./pages/client/MyDevicesPage";
import Register from "./pages/Register";
import DeviceDetailsPage from "./pages/client/DeviceDetailsPage";

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <RouteButtons />
        <Routes>
          <Route path="/login" element={<Login/>}/>
          <Route path="/register" element={<Register/>}/>
          <Route element={<RequireAuth/>}>
            <Route index element={<Dashboard/>} />
            <Route element={<RequireRole allowed={["ADMIN"]} />}>
              <Route path="/admin/users" element={<UsersPage/>}/>
              <Route path="/admin/devices" element={<DevicesPage/>}/>
              <Route path="/admin/assignments" element={<AssignmentsPage/>}/>
            </Route>
            <Route element={<RequireRole allowed={["CLIENT"]} />}>
              <Route path="/me/devices" element={<MyDevicesPage/>}/>
              <Route path="/client/device/:deviceId" element={<DeviceDetailsPage/>}/>
            </Route>
          </Route>
          <Route path="*" element={<Navigate to="/" replace/>}/>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

function RouteButtons() {
  const { role } = useAuth();
  const nav = useNavigate();
  if (!role) return null;
  return (
    <div style={{ display: "flex", gap: 12, justifyContent: "center", padding: "16px 0", background: "#f8f9fa", boxShadow: "0 2px 8px #0001", marginBottom: 8 }}>
      <button onClick={()=>nav("/")} style={btnStyle}>Dashboard</button>
      {role === "ADMIN" && <>
        <button onClick={()=>nav("/admin/users")} style={btnStyle}>Users</button>
        <button onClick={()=>nav("/admin/devices")} style={btnStyle}>Devices</button>
        <button onClick={()=>nav("/admin/assignments")} style={btnStyle}>Assignments</button>
      </>}
      {role === "CLIENT" && <>
        <button onClick={()=>nav("/me/devices")} style={btnStyle}>My Devices</button>
      </>}
    </div>
  );
}

const btnStyle = {
  padding: "8px 20px",
  borderRadius: 8,
  background: "#007bff",
  color: "#fff",
  border: "none",
  fontWeight: 500,
  fontSize: 16,
  cursor: "pointer",
  boxShadow: "0 2px 8px #007bff22"
};
