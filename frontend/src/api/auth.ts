// src/api/auth.ts
import { authApi } from "./http";

export async function loginApi(username: string, password: string) {
  // Expect your user-service: POST /auth/login -> { token }
  const { data } = await authApi.post("api/auth/login", { username, password });
  return data as { token: string }; 
}
