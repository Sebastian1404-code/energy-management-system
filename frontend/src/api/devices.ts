// src/api/devices.ts
import { deviceApi } from "./http";

export type Device = { id: number; name: string; maximConsumptionValue: number; userId?: number };

export const DevicesApi = {
  list: async () => (await deviceApi.get<Device[]>("/api/devices")).data,
  create: async (d: Partial<Device>) => (await deviceApi.post<Device>("/api/devices", d)).data,
  update: async (id: number, d: Partial<Device>) => (await deviceApi.put<Device>(`/api/devices/${id}`, d)).data,
  remove: async (id: number) => deviceApi.delete(`/api/devices/${id}`),
  // assignment: either PATCH device user or dedicated endpoint; both patterns shown:
  assignToUser: async (deviceId: number, userId: number) =>
    (await deviceApi.put<Device>(`/api/devices/${deviceId}`, { userId })).data,
  // or if you exposed: POST /api/assignments { userId, deviceId }
};
