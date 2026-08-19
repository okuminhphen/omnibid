"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { GoogleOneTap } from "@/components/GoogleOneTap";
import { useAuth } from "@/components/AuthProvider";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";

const DEV_ACCOUNTS = [
  { alias: "customer-a", label: "Customer A", note: "Dùng cửa sổ thường" },
  { alias: "customer-b", label: "Customer B", note: "Dùng cửa sổ ẩn danh" },
  { alias: "admin", label: "Administrator", note: "Role ADMIN" }
];

export default function LoginPage() {
  const router = useRouter();
  const { user, hydrated, loginDev } = useAuth();
  const [submitting, setSubmitting] = useState<string | null>(null);
  const [error, setError] = useState("");
  const finishLogin = useCallback(() => router.replace("/"), [router]);

  useEffect(() => {
    if (hydrated && user) finishLogin();
  }, [finishLogin, hydrated, user]);

  async function login(alias: string) {
    setSubmitting(alias);
    setError("");
    try {
      await loginDev(alias);
      finishLogin();
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Đăng nhập thất bại.");
    } finally {
      setSubmitting(null);
    }
  }

  return (
    <main className="fine-grid min-h-[calc(100vh-4rem)] px-5 py-12 sm:py-20">
      <Card className="mx-auto max-w-lg overflow-hidden shadow-2xl shadow-slate-200/70">
        <div className="bg-slate-950 px-7 py-8 text-white sm:px-10">
          <p className="text-xs font-bold uppercase tracking-[0.2em] text-orange-400">Secure identity boundary</p>
          <h1 className="mt-3 text-3xl font-black tracking-tight">Đăng nhập OmniBid</h1>
          <p className="mt-3 text-sm leading-6 text-slate-400">Access token sống ngắn nằm trong memory; refresh token được bảo vệ trong HttpOnly cookie và xoay vòng sau mỗi lần dùng.</p>
        </div>
        <CardContent className="space-y-7 p-7 sm:p-10">
          <GoogleOneTap onSuccess={finishLogin} />

          <div className="flex items-center gap-3 text-xs font-bold uppercase tracking-[0.14em] text-slate-400">
            <span className="h-px flex-1 bg-slate-200" />
            Local concurrency lab
            <span className="h-px flex-1 bg-slate-200" />
          </div>

          <div className="space-y-3">
            {DEV_ACCOUNTS.map((account) => (
              <Button
                key={account.alias}
                variant="outline"
                disabled={Boolean(submitting)}
                onClick={() => void login(account.alias)}
                className="h-auto w-full justify-between px-4 py-3"
              >
                <span className="text-left">
                  <span className="block font-black text-slate-900">{account.label}</span>
                  <span className="mt-0.5 block text-xs font-medium text-slate-500">{account.note}</span>
                </span>
                <span>{submitting === account.alias ? "Đang vào..." : "→"}</span>
              </Button>
            ))}
          </div>

          {error && <p className="rounded-xl bg-rose-50 px-4 py-3 text-sm font-medium text-rose-700">{error}</p>}
          <p className="text-xs leading-5 text-slate-500">Tài khoản dev chỉ tồn tại ở Spring profile <code>local</code> và không được bật trong production.</p>
        </CardContent>
      </Card>
    </main>
  );
}
