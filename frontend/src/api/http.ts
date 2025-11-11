// src/api/http.ts
import axios from "axios";

const userApi = axios.create({ baseURL: import.meta.env.VITE_API_URL });
const deviceApi = axios.create({ baseURL: import.meta.env.VITE_DEVICE_API_URL });
const authApi = axios.create({ baseURL: import.meta.env.VITE_AUTH_API_URL });

const attachAuth = (instance: typeof userApi) => {
  instance.interceptors.request.use(cfg => {
    const token = localStorage.getItem("token");
    if (token) cfg.headers.Authorization = `Bearer ${token}`;
    return cfg;
  });
  instance.interceptors.response.use(
    r => r,
    err => {
      if (err?.response?.status === 401) {
        localStorage.removeItem("token");
        window.location.href = "/login";
      }
      return Promise.reject(err);
    }
  );
};
attachAuth(userApi);
attachAuth(deviceApi);
attachAuth(authApi);

export { userApi, deviceApi, authApi };
