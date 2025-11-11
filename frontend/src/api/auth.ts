// src/api/auth.ts
import { authApi } from "./http";

export async function loginApi(username: string, password: string) {
  // Expect your user-service: POST /auth/login -> { token }
  const { data } = await authApi.post("api/auth/login", { username, password });
  return data as { token: string }; 
}

export async function registerApi(username: string, password: string, role: "CLIENT"|"ADMIN") {
  // Expect your user-service: POST /auth/register -> { id, username, role }
  const { data } = await authApi.post("api/auth/register", { username, password, role });
  return data;
}
