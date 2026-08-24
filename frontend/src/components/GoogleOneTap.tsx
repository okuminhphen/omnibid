"use client";

import Script from "next/script";
import { useCallback, useEffect, useRef, useState } from "react";
import { getGoogleAuthConfig, loginWithGoogle } from "@/services/authSession";
import type { GoogleAuthConfig } from "@/types/auth";

interface GoogleCredentialResponse {
  credential: string;
}

interface GoogleAccountsId {
  initialize: (options: {
    client_id: string;
    callback: (response: GoogleCredentialResponse) => void;
    nonce: string;
    auto_select?: boolean;
    cancel_on_tap_outside?: boolean;
  }) => void;
  renderButton: (parent: HTMLElement, options: Record<string, string>) => void;
  prompt: () => void;
  cancel: () => void;
}

declare global {
  interface Window {
    google?: { accounts: { id: GoogleAccountsId } };
  }
}

export function GoogleOneTap({ onSuccess }: { onSuccess: () => void }) {
  const buttonRef = useRef<HTMLDivElement>(null);
  const initializedForNonce = useRef<string | null>(null);
  const [config, setConfig] = useState<GoogleAuthConfig | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    getGoogleAuthConfig()
      .then(setConfig)
      .catch((requestError: unknown) => {
        setError(requestError instanceof Error ? requestError.message : "Không tải được cấu hình Google.");
      });
  }, []);

  const initialize = useCallback(() => {
    if (!config?.enabled || !config.clientId || !window.google || !buttonRef.current) return;
    if (initializedForNonce.current === config.nonce) return;

    window.google.accounts.id.initialize({
      client_id: config.clientId,
      nonce: config.nonce,
      auto_select: false,
      cancel_on_tap_outside: true,
      callback: (response) => {
        setError("");
        void loginWithGoogle(response.credential, config.nonce)
          .then(onSuccess)
          .catch((requestError: unknown) => {
            setError(requestError instanceof Error ? requestError.message : "Google login thất bại.");
          });
      }
    });
    buttonRef.current.replaceChildren();
    window.google.accounts.id.renderButton(buttonRef.current, {
      type: "standard",
      theme: "outline",
      size: "large",
      shape: "rectangular",
      text: "continue_with",
      width: "360"
    });
    initializedForNonce.current = config.nonce;
    window.google.accounts.id.prompt();
  }, [config, onSuccess]);

  useEffect(() => {
    initialize();
    return () => window.google?.accounts.id.cancel();
  }, [initialize]);

  if (config && !config.enabled) {
    return (
      <p className="border border-amber-200 bg-amber-50 px-4 py-3 text-center text-xs leading-5 text-amber-800">
        Đăng nhập Google chưa được cấu hình cho môi trường này. Xem hướng dẫn thiết lập trong README.
      </p>
    );
  }

  return (
    <div className="space-y-3">
      {config?.enabled && (
        <Script
          id="google-identity-services"
          src="https://accounts.google.com/gsi/client"
          strategy="afterInteractive"
          onReady={initialize}
          onError={() => setError("Không tải được Google Identity Services.")}
        />
      )}
      <div ref={buttonRef} className="flex min-h-11 justify-center" />
      {error && <p className="text-center text-xs font-medium text-rose-600">{error}</p>}
    </div>
  );
}
