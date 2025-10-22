// src/App.tsx
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider } from "./auth/AuthContext";
import RequireAuth from "./auth/RequireAuth";
import RequireRole from "./auth/RequireRole";
import Login from "./pages/Login";
import Dashboard from "./pages/Dashboard";
import UsersPage from "./pages/admin/UsersPage";
import DevicesPage from "./pages/admin/DevicesPage";
import AssignmentsPage from "./pages/admin/AssignmentsPage";
import MyDevicesPage from "./pages/client/MyDevicesPage";

export default function App(){
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login/>}/>
          <Route element={<RequireAuth/>}>
            <Route index element={<Dashboard/>} />
            <Route element={<RequireRole allowed={["ADMIN"]} />}>
              <Route path="/admin/users" element={<UsersPage/>}/>
              <Route path="/admin/devices" element={<DevicesPage/>}/>
              <Route path="/admin/assignments" element={<AssignmentsPage/>}/>
            </Route>
            <Route element={<RequireRole allowed={["CLIENT"]} />}>
              <Route path="/me/devices" element={<MyDevicesPage/>}/>
            </Route>
          </Route>
          <Route path="*" element={<Navigate to="/" replace/>}/>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
