import type { AuthResponse, AuthState, GoogleAuthConfig } from "@/types/auth";

const IDENTITY_API_URL =
  process.env.NEXT_PUBLIC_IDENTITY_API_URL ?? "http://localhost:8083";

type AuthListener = (state: AuthState) => void;

let authState: AuthState = {
  accessToken: null,
  user: null,
  hydrated: false
};
let refreshPromise: Promise<string | null> | null = null;
const listeners = new Set<AuthListener>();

function publish(nextState: AuthState): void {
  authState = nextState;
  listeners.forEach((listener) => listener(authState));
}

async function readError(response: Response): Promise<string> {
  try {
    const body = (await response.json()) as { message?: string };
    return body.message ?? `Request failed (${response.status})`;
  } catch {
    return `Request failed (${response.status})`;
  }
}

async function requestAuth(path: string, init: RequestInit = {}): Promise<AuthResponse> {
  const response = await fetch(`${IDENTITY_API_URL}${path}`, {
    ...init,
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      ...init.headers
    }
  });
  if (!response.ok) {
    throw new Error(await readError(response));
  }
  return response.json() as Promise<AuthResponse>;
}

function applyAuthResponse(response: AuthResponse): string {
  publish({
    accessToken: response.accessToken,
    user: response.user,
    hydrated: true
  });
  return response.accessToken;
}

export function getAuthState(): AuthState {
  return authState;
}

export function getAccessToken(): string | null {
  return authState.accessToken;
}

export function subscribeAuth(listener: AuthListener): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function updateAuthUser(user: AuthState["user"]): void {
  publish({ ...authState, user });
}

export function clearAuthSession(): void {
  publish({ accessToken: null, user: null, hydrated: true });
}

export async function refreshAuthSession(): Promise<string | null> {
  if (refreshPromise) return refreshPromise;

  refreshPromise = requestAuth("/api/v1/auth/refresh", { method: "POST" })
    .then(applyAuthResponse)
    .catch(() => {
      clearAuthSession();
      return null;
    })
    .finally(() => {
      refreshPromise = null;
    });
  return refreshPromise;
}

export async function bootstrapAuthSession(): Promise<void> {
  if (!authState.hydrated) await refreshAuthSession();
}

export async function loginWithGoogle(credential: string, nonce: string): Promise<void> {
  const response = await requestAuth("/api/v1/auth/google", {
    method: "POST",
    body: JSON.stringify({ credential, nonce })
  });
  applyAuthResponse(response);
}

export async function getGoogleAuthConfig(): Promise<GoogleAuthConfig> {
  const response = await fetch(`${IDENTITY_API_URL}/api/v1/auth/google/config`, {
    credentials: "include",
    cache: "no-store"
  });
  if (!response.ok) throw new Error(await readError(response));
  return response.json() as Promise<GoogleAuthConfig>;
}

export async function logoutAuthSession(): Promise<void> {
  try {
    await fetch(`${IDENTITY_API_URL}/api/v1/auth/logout`, {
      method: "POST",
      credentials: "include",
      headers: authState.accessToken
        ? { Authorization: `Bearer ${authState.accessToken}` }
        : undefined
    });
  } finally {
    clearAuthSession();
  }
}
