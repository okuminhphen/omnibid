import axios, {
  AxiosHeaders,
  type AxiosError,
  type AxiosInstance,
  type InternalAxiosRequestConfig
} from "axios";
import { getAccessToken, refreshAuthSession } from "@/services/authSession";

const JSON_HEADERS = { "Content-Type": "application/json" };

export const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_AUCTION_API_URL ?? "http://localhost:8080",
  timeout: 8_000,
  headers: JSON_HEADERS
});

export const walletApi = axios.create({
  baseURL: process.env.NEXT_PUBLIC_WALLET_API_URL ?? "http://localhost:8081",
  timeout: 8_000,
  headers: JSON_HEADERS
});

export const identityApi = axios.create({
  baseURL: process.env.NEXT_PUBLIC_IDENTITY_API_URL ?? "http://localhost:8083",
  timeout: 8_000,
  withCredentials: true,
  headers: JSON_HEADERS
});

function needsIdempotencyKey(config: InternalAxiosRequestConfig): boolean {
  const method = config.method?.toLowerCase();
  const url = config.url ?? "";
  return method === "post" && /\/(bid|bids|payments?|top-up|top-ups|withdrawals)(?:\/|\?|$)/.test(url);
}

function addIdempotencyKey(config: InternalAxiosRequestConfig): InternalAxiosRequestConfig {
  if (!needsIdempotencyKey(config)) {
    return config;
  }

  const headers = AxiosHeaders.from(config.headers);
  if (!headers.has("X-Idempotency-Key")) {
    headers.set("X-Idempotency-Key", crypto.randomUUID());
  }
  config.headers = headers;
  return config;
}

function installRequestInterceptors(instance: AxiosInstance): void {
  instance.interceptors.request.use((config) => {
    const token = getAccessToken();
    const headers = AxiosHeaders.from(config.headers);
    if (token && !headers.has("Authorization")) {
      headers.set("Authorization", `Bearer ${token}`);
    }
    config.headers = headers;
    return addIdempotencyKey(config);
  });

  instance.interceptors.response.use(
    (response) => response,
    async (error: AxiosError) => {
      const config = error.config as (InternalAxiosRequestConfig & { _authRetried?: boolean }) | undefined;
      if (error.response?.status !== 401 || !config || config._authRetried) {
        return Promise.reject(error);
      }

      config._authRetried = true;
      const nextToken = await refreshAuthSession();
      if (!nextToken) return Promise.reject(error);

      const headers = AxiosHeaders.from(config.headers);
      headers.set("Authorization", `Bearer ${nextToken}`);
      config.headers = headers;
      return instance.request(config);
    }
  );
}

installRequestInterceptors(api);
installRequestInterceptors(walletApi);
installRequestInterceptors(identityApi);
