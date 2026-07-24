import axios from "axios";
import {
  clearSession,
  getAccessToken,
  saveSession,
} from "../utils/session";

export const API_BASE_URL = (
  import.meta.env.VITE_API_URL || "http://localhost:2424"
).replace(/\/+$/, "");

const httpClient = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
  timeout: 20000,
  headers: { Accept: "application/json" },
});

let refreshPromise = null;

httpClient.interceptors.request.use((config) => {
  const token = getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

httpClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    const status = error.response?.status;
    const code = error.response?.data?.code;
    const isAuthEndpoint = originalRequest?.url?.includes("/auth/");

    if (
      status === 401 &&
      code === "TOKEN_EXPIRED" &&
      !originalRequest?._retry &&
      !isAuthEndpoint
    ) {
      originalRequest._retry = true;
      refreshPromise ??= axios
        .post(
          `${API_BASE_URL}/auth/refresh`,
          {},
          { withCredentials: true, timeout: 20000 },
        )
        .then(({ data }) => saveSession(data))
        .finally(() => {
          refreshPromise = null;
        });

      try {
        const session = await refreshPromise;
        originalRequest.headers.Authorization = `Bearer ${session.accessToken}`;
        return httpClient(originalRequest);
      } catch (refreshError) {
        clearSession();
        return Promise.reject(refreshError);
      }
    }

    if (
      status === 401 &&
      !isAuthEndpoint &&
      ["TOKEN_INVALID", "ACCOUNT_DISABLED"].includes(code)
    ) {
      clearSession();
    }

    return Promise.reject(error);
  },
);

export default httpClient;
