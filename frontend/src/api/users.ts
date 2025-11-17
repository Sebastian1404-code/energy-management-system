// src/api/users.ts
import { userApi } from "./http";

export type User = { id: number; username: string; email?: string; role: "ADMIN" | "CLIENT"; password?: string };

export const UsersApi = {
  list: async () => (await userApi.get<User[]>("")).data,
  create: async (u: Partial<User>) => (await userApi.post<User>("/create", u)).data,
  update: async (id: number, u: Partial<User>) => (await userApi.put<User>(`/${id}`, u)).data,
  remove: async (id: number) => userApi.delete(`/${id}`),
  getById: async (username: string) => (await userApi.post<number>(`/id-by-username`, { username })).data
};
