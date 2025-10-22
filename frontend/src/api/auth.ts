// src/api/auth.ts
import { userApi } from "./http";

export async function loginApi(username: string, password: string) {
  // Expect your user-service: POST /auth/login -> { token }
  const { data } = await userApi.post("/auth/login", { username, password });
  return data as { token: string }; 
}
