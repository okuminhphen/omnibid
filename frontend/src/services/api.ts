import axios, { AxiosHeaders, type AxiosInstance, type InternalAxiosRequestConfig } from "axios";

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

function needsIdempotencyKey(config: InternalAxiosRequestConfig): boolean {
  const method = config.method?.toLowerCase();
  const url = config.url ?? "";
  return method === "post" && /\/(bid|bids|payments?|top-up)(?:\/|\?|$)/.test(url);
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
  instance.interceptors.request.use(addIdempotencyKey);
}

installRequestInterceptors(api);
installRequestInterceptors(walletApi);
