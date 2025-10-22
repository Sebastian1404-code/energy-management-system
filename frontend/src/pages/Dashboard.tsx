// src/pages/Dashboard.tsx
import { useAuth } from "../auth/AuthContext";
export default function Dashboard(){
  const { username, role } = useAuth();
  return (
    <div style={{ padding: 24 }}>
      <h2>Welcome {username}</h2>
      <p>Your role: {role || "none"}</p>
    </div>
  );
}
