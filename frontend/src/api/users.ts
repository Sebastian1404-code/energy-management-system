// src/api/users.ts
import { userApi } from "./http";

export type User = { id: number; username: string; role: "ADMIN" | "CLIENT"; password?: string };

export const UsersApi = {
  list: async () => (await userApi.get<User[]>("/api/users")).data,
  create: async (u: Partial<User>) => (await userApi.post<User>("/api/users", u)).data,
  update: async (id: number, u: Partial<User>) => (await userApi.put<User>(`/api/users/${id}`, u)).data,
  remove: async (id: number) => userApi.delete(`/api/users/${id}`)
};
