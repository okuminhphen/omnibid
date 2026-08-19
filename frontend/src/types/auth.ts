export type UserRole = "CUSTOMER" | "ADMIN";

export interface AuthUser {
  id: string;
  email: string;
  displayName: string;
  avatarUrl: string | null;
  phoneNumber: string | null;
  locale: string;
  timezone: string;
  bio: string | null;
  status: "ACTIVE" | "BLOCKED" | "DELETED";
  roles: UserRole[];
}

export interface AuthResponse {
  accessToken: string;
  tokenType: "Bearer";
  expiresIn: number;
  user: AuthUser;
}

export interface AuthState {
  accessToken: string | null;
  user: AuthUser | null;
  hydrated: boolean;
}

export interface GoogleAuthConfig {
  enabled: boolean;
  clientId: string;
  nonce: string;
}

export interface UpdateProfileRequest {
  displayName: string;
  phoneNumber: string;
  locale: string;
  timezone: string;
  bio: string;
}

export interface AuthSessionInfo {
  id: string;
  userAgent: string | null;
  status: "ACTIVE" | "ROTATED" | "REVOKED" | "EXPIRED";
  createdAt: string;
  lastUsedAt: string | null;
  expiresAt: string;
  current: boolean;
}
