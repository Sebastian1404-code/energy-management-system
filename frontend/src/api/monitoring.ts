// src/api/monitoring.ts
import { monitoringApi } from "./http";

// Example: Get device summaries
export const getDeviceSummaries = async () => {
  const resp = await monitoringApi.get("");
  return resp.data;
};

// Example: Get device series by ID and date
export const getDeviceSeries = async (deviceId: string, date: string) => {
  const url = `/devices/${deviceId}/series?date=${date}&tz=Europe/Bucharest&virtualHourMinutes=1`;
  const resp = await monitoringApi.get(url);
  return resp.data;
};

// Example: Get user monitoring summary
export const getUserMonitoringSummary = async (userId: string) => {
  const resp = await monitoringApi.get(`/users/${userId}/summary`);
  return resp.data;
};
