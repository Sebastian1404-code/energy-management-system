// src/api/devices.ts
import { deviceApi } from "./http";

export type Device = { id: number; name: string; maximConsumptionValue: number; userId?: number };

export const DevicesApi = {
  list: async () => (await deviceApi.get<Device[]>("")).data,
  create: async (d: Partial<Device>) => (await deviceApi.post<Device>("", d)).data,
  update: async (id: number, d: Partial<Device>) => (await deviceApi.put<Device>(`/${id}`, d)).data,
  remove: async (id: number) => deviceApi.delete(`/${id}`),
  assignToUser: async (deviceId: number, userId: number) =>
    (await deviceApi.put<Device>(`/${deviceId}`, userId)).data,
};
