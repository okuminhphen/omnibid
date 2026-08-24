"use client";

import Link from "next/link";
import { useCallback, useEffect, useState, type FormEvent } from "react";
import { useAuth } from "@/components/AuthProvider";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { getApiErrorMessage } from "@/lib/errors";
import { identityApi } from "@/services/api";
import { clearAuthSession, updateAuthUser } from "@/services/authSession";
import type { AuthSessionInfo, AuthUser, UpdateProfileRequest } from "@/types/auth";

export default function ProfilePage() {
  const { user, hydrated } = useAuth();
  const [form, setForm] = useState<UpdateProfileRequest>({
    displayName: "",
    phoneNumber: "",
    locale: "vi-VN",
    timezone: "Asia/Ho_Chi_Minh",
    bio: ""
  });
  const [sessions, setSessions] = useState<AuthSessionInfo[]>([]);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    if (!user) return;
    setForm({
      displayName: user.displayName,
      phoneNumber: user.phoneNumber ?? "",
      locale: user.locale,
      timezone: user.timezone,
      bio: user.bio ?? ""
    });
  }, [user]);

  const loadSessions = useCallback(async () => {
    if (!user) return;
    try {
      const response = await identityApi.get<AuthSessionInfo[]>("/api/v1/me/sessions");
      setSessions(response.data);
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, "Không tải được danh sách phiên đăng nhập."));
    }
  }, [user]);

  useEffect(() => {
    void loadSessions();
  }, [loadSessions]);

  async function saveProfile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setError("");
    setMessage("");
    try {
      const response = await identityApi.patch<AuthUser>("/api/v1/me/profile", form);
      updateAuthUser(response.data);
      setMessage("Hồ sơ đã được cập nhật.");
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, "Không thể cập nhật hồ sơ."));
    } finally {
      setSaving(false);
    }
  }

  async function revokeSession(sessionId: string) {
    try {
      await identityApi.delete(`/api/v1/me/sessions/${sessionId}`);
      await loadSessions();
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, "Không thể thu hồi phiên đăng nhập."));
    }
  }

  async function logoutEverywhere() {
    try {
      await identityApi.post("/api/v1/me/logout-all");
    } finally {
      clearAuthSession();
      window.location.assign("/login");
    }
  }

  if (!hydrated) {
    return <main className="mx-auto max-w-5xl px-5 py-20 text-center text-stone-500">Đang khôi phục phiên đăng nhập...</main>;
  }

  if (!user) {
    return (
      <main className="mx-auto max-w-xl px-5 py-20 text-center">
        <h1 className="display-serif text-4xl text-stone-950">Bạn chưa đăng nhập</h1>
        <Link href="/login" className="mt-6 inline-block rounded bg-stone-950 px-6 py-3 font-semibold text-white">Đến trang đăng nhập</Link>
      </main>
    );
  }

  return (
    <main className="mx-auto max-w-[1200px] px-5 py-10 sm:px-8 sm:py-16">
      <div className="mb-10 border-b border-stone-300 pb-7">
        <p className="text-[10px] font-semibold uppercase tracking-[0.2em] text-[#b43a2f]">Tài khoản cá nhân</p>
        <h1 className="display-serif mt-3 text-5xl tracking-[-0.035em] text-stone-950">Hồ sơ & bảo mật</h1>
        <p className="mt-4 text-sm text-stone-600">{user.email} · {user.id}</p>
        <div className="mt-3 flex gap-2">
          {user.roles.map((role) => <span key={role} className="border border-stone-400 px-2.5 py-1 text-[10px] font-semibold uppercase tracking-[0.12em] text-stone-700">{role}</span>)}
        </div>
      </div>

      {error && <p className="mb-5 border border-red-300 bg-red-50 p-4 text-sm text-red-800">{error}</p>}
      {message && <p className="mb-5 border border-emerald-300 bg-emerald-50 p-4 text-sm text-emerald-800">{message}</p>}

      <div className="grid gap-6 lg:grid-cols-[1fr_0.85fr]">
        <Card>
          <CardContent className="p-7">
            <h2 className="display-serif text-2xl text-stone-950">Thông tin cá nhân</h2>
            <form onSubmit={saveProfile} className="mt-6 grid gap-5 sm:grid-cols-2">
              <Field label="Tên hiển thị" className="sm:col-span-2">
                <Input value={form.displayName} required maxLength={120} onChange={(event) => setForm({ ...form, displayName: event.target.value })} />
              </Field>
              <Field label="Số điện thoại">
                <Input value={form.phoneNumber} maxLength={30} onChange={(event) => setForm({ ...form, phoneNumber: event.target.value })} />
              </Field>
              <Field label="Ngôn ngữ">
                <Input value={form.locale} required maxLength={20} onChange={(event) => setForm({ ...form, locale: event.target.value })} />
              </Field>
              <Field label="Múi giờ" className="sm:col-span-2">
                <Input value={form.timezone} required maxLength={50} onChange={(event) => setForm({ ...form, timezone: event.target.value })} />
              </Field>
              <Field label="Giới thiệu" className="sm:col-span-2">
                <textarea
                  value={form.bio}
                  maxLength={500}
                  rows={4}
                  onChange={(event) => setForm({ ...form, bio: event.target.value })}
                  className="w-full rounded border border-stone-400 bg-[#fffdf9] px-4 py-3 text-sm outline-none focus:border-[#b43a2f] focus:ring-2 focus:ring-[#b43a2f]/10"
                />
              </Field>
              <Button disabled={saving} className="sm:col-span-2">{saving ? "Đang lưu..." : "Lưu hồ sơ"}</Button>
            </form>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-7">
            <div className="flex items-start justify-between gap-4">
              <div>
                <h2 className="display-serif text-2xl text-stone-950">Phiên đăng nhập</h2>
                <p className="mt-2 text-sm leading-6 text-stone-500">Xem và thu hồi quyền truy cập trên từng thiết bị.</p>
              </div>
              <Button variant="outline" onClick={() => void logoutEverywhere()} className="shrink-0 text-rose-600">Thu hồi tất cả</Button>
            </div>

            <div className="mt-6 space-y-3">
              {sessions.map((session) => (
                <div key={session.id} className="border-b border-stone-300 py-4">
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <p className="truncate text-sm font-semibold text-stone-800">{session.userAgent || "Unknown client"}</p>
                      <p className="mt-1 text-xs text-stone-500">
                        {session.lastUsedAt ? "Dùng gần nhất" : "Đăng nhập lúc"} {new Date(session.lastUsedAt ?? session.createdAt).toLocaleString("vi-VN")}
                      </p>
                    </div>
                    {session.current ? (
                      <span className="border border-emerald-300 px-2.5 py-1 text-[9px] font-semibold uppercase tracking-[0.12em] text-emerald-700">Hiện tại</span>
                    ) : (
                      <button type="button" onClick={() => void revokeSession(session.id)} className="text-xs font-bold text-rose-600 hover:underline">Thu hồi</button>
                    )}
                  </div>
                </div>
              ))}
              {sessions.length === 0 && <p className="py-8 text-center text-sm text-slate-500">Chưa có dữ liệu phiên.</p>}
            </div>
          </CardContent>
        </Card>
      </div>
    </main>
  );
}

function Field({ label, className = "", children }: { label: string; className?: string; children: React.ReactNode }) {
  return (
    <label className={className}>
      <span className="mb-2 block text-[10px] font-semibold uppercase tracking-[0.12em] text-stone-500">{label}</span>
      {children}
    </label>
  );
}
