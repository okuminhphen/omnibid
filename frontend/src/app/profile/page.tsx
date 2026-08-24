"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useState, type FormEvent, type ReactNode } from "react";
import { useAuth } from "@/components/AuthProvider";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { getApiErrorMessage } from "@/lib/errors";
import { identityApi } from "@/services/api";
import { clearAuthSession, updateAuthUser } from "@/services/authSession";
import type { AuthSessionInfo, AuthUser, UpdateProfileRequest } from "@/types/auth";

const LOCALES = [
  { value: "vi-VN", label: "Tiếng Việt" },
  { value: "en-US", label: "English" }
];

const TIMEZONES = [
  { value: "Asia/Ho_Chi_Minh", label: "Hồ Chí Minh (GMT+7)" },
  { value: "Asia/Bangkok", label: "Bangkok (GMT+7)" },
  { value: "Asia/Singapore", label: "Singapore (GMT+8)" }
];

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

  const sortedSessions = useMemo(
    () => sessions
      .filter((session) => session.status === "ACTIVE")
      .sort((left, right) => Number(right.current) - Number(left.current)),
    [sessions]
  );

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
    setError("");
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
        <p className="mt-3 text-sm leading-6 text-stone-500">Đăng nhập để xem và cập nhật hồ sơ cá nhân.</p>
        <Link href="/login" className="mt-6 inline-block rounded bg-stone-950 px-6 py-3 font-semibold text-white">Đến trang đăng nhập</Link>
      </main>
    );
  }

  return (
    <main className="mx-auto max-w-[1080px] px-5 py-10 sm:px-8 sm:py-16">
      <header className="border-b border-stone-300 pb-8">
        <p className="text-[10px] font-semibold uppercase tracking-[0.2em] text-[#b43a2f]">Identity service</p>
        <div className="mt-5 flex flex-col gap-5 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-4">
            <div className="display-serif grid size-16 shrink-0 place-items-center rounded-full border border-stone-300 bg-[#fffdf9] text-2xl text-stone-900">
              {getInitials(user.displayName)}
            </div>
            <div className="min-w-0">
              <h1 className="display-serif text-4xl tracking-[-0.035em] text-stone-950 sm:text-5xl">Hồ sơ & bảo mật</h1>
              <p className="mt-2 truncate text-sm text-stone-500">{user.email}</p>
            </div>
          </div>
          <div className="flex items-center gap-2 self-start sm:self-auto">
            {user.roles.map((role) => (
              <span key={role} className="border border-stone-400 px-2.5 py-1 text-[10px] font-semibold uppercase tracking-[0.12em] text-stone-700">
                {role === "ADMIN" ? "Quản trị viên" : "Khách hàng"}
              </span>
            ))}
          </div>
        </div>
      </header>

      <div aria-live="polite" className="mt-6 space-y-3">
        {error && <p className="border border-red-300 bg-red-50 p-4 text-sm text-red-800">{error}</p>}
        {message && <p className="border border-emerald-300 bg-emerald-50 p-4 text-sm text-emerald-800">{message}</p>}
      </div>

      <div className="mt-8 space-y-8">
        <section className="border border-stone-300 bg-[#fffdf9]">
          <div className="border-b border-stone-300 px-5 py-6 sm:px-8">
            <h2 className="display-serif text-3xl text-stone-950">Thông tin cá nhân</h2>
            <p className="mt-2 max-w-2xl text-sm leading-6 text-stone-500">
              Thông tin này được dùng để nhận diện tài khoản và hiển thị trong các hoạt động của bạn trên OmniBid.
            </p>
          </div>

          <div className="grid border-b border-stone-300 bg-stone-50/70 sm:grid-cols-2">
            <AccountFact label="Email đăng nhập" value={user.email} />
            <AccountFact label="Mã tài khoản" value={user.id} className="sm:border-l sm:border-stone-300" monospace />
          </div>

          <form onSubmit={saveProfile} className="px-5 py-7 sm:px-8 sm:py-8">
            <div className="grid gap-x-6 gap-y-6 sm:grid-cols-2">
              <Field label="Tên hiển thị" hint="Tên xuất hiện trên hồ sơ của bạn.">
                <Input value={form.displayName} required maxLength={120} autoComplete="name" onChange={(event) => setForm({ ...form, displayName: event.target.value })} />
              </Field>
              <Field label="Số điện thoại" hint="Không công khai với người tham gia khác.">
                <Input value={form.phoneNumber} maxLength={30} inputMode="tel" autoComplete="tel" placeholder="Ví dụ: 0912 345 678" onChange={(event) => setForm({ ...form, phoneNumber: event.target.value })} />
              </Field>
              <Field label="Ngôn ngữ">
                <Select value={form.locale} onChange={(value) => setForm({ ...form, locale: value })} options={LOCALES} />
              </Field>
              <Field label="Múi giờ">
                <Select value={form.timezone} onChange={(value) => setForm({ ...form, timezone: value })} options={TIMEZONES} />
              </Field>
              <Field label="Giới thiệu" hint="Tối đa 500 ký tự." className="sm:col-span-2">
                <textarea
                  value={form.bio}
                  maxLength={500}
                  rows={4}
                  placeholder="Một vài dòng về sở thích sưu tầm của bạn..."
                  onChange={(event) => setForm({ ...form, bio: event.target.value })}
                  className="w-full resize-y rounded border border-stone-400 bg-[#fffdf9] px-4 py-3 text-sm leading-6 outline-none placeholder:text-stone-400 focus:border-[#b43a2f] focus:ring-2 focus:ring-[#b43a2f]/10"
                />
                <p className="mt-1 text-right text-xs tabular-nums text-stone-400">{form.bio.length}/500</p>
              </Field>
            </div>
            <div className="mt-7 flex justify-end border-t border-stone-200 pt-6">
              <Button disabled={saving} className="min-w-36">{saving ? "Đang lưu..." : "Lưu thay đổi"}</Button>
            </div>
          </form>
        </section>

        <section className="border border-stone-300 bg-[#fffdf9]">
          <div className="flex flex-col gap-4 border-b border-stone-300 px-5 py-6 sm:flex-row sm:items-start sm:justify-between sm:px-8">
            <div>
              <h2 className="display-serif text-3xl text-stone-950">Thiết bị đã đăng nhập</h2>
              <p className="mt-2 text-sm leading-6 text-stone-500">Kiểm tra các phiên đang hoạt động và thu hồi thiết bị bạn không nhận ra.</p>
            </div>
            <Button variant="outline" onClick={() => void logoutEverywhere()} className="self-start text-rose-700">Đăng xuất mọi thiết bị</Button>
          </div>

          <div className="divide-y divide-stone-200 px-5 sm:px-8">
            {sortedSessions.map((session) => {
              const device = describeUserAgent(session.userAgent);
              const activityAt = session.lastUsedAt ?? session.createdAt;
              return (
                <div key={session.id} className="flex items-center gap-4 py-5">
                  <div className="grid size-11 shrink-0 place-items-center rounded-full border border-stone-300 bg-stone-50 text-sm font-semibold text-stone-700">{device.icon}</div>
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <p className="font-semibold text-stone-900">{device.title}</p>
                      {session.current && <span className="bg-emerald-50 px-2 py-1 text-[9px] font-semibold uppercase tracking-[0.1em] text-emerald-700">Thiết bị này</span>}
                    </div>
                    <p className="mt-1 text-xs text-stone-500">{session.lastUsedAt ? "Hoạt động" : "Đăng nhập"} {formatRelativeDate(activityAt)}</p>
                  </div>
                  {!session.current && (
                    <button type="button" onClick={() => void revokeSession(session.id)} className="shrink-0 text-xs font-semibold text-[#b43a2f] hover:underline">Thu hồi</button>
                  )}
                </div>
              );
            })}
            {sortedSessions.length === 0 && <p className="py-10 text-center text-sm text-stone-500">Không có phiên đăng nhập đang hoạt động.</p>}
          </div>
        </section>
      </div>
    </main>
  );
}

function Field({ label, hint, className = "", children }: { label: string; hint?: string; className?: string; children: ReactNode }) {
  return (
    <label className={className}>
      <span className="block text-sm font-semibold text-stone-800">{label}</span>
      {hint ? <span className="mb-2 mt-1 block text-xs leading-5 text-stone-500">{hint}</span> : <span className="block h-2" />}
      {children}
    </label>
  );
}

function Select({ value, onChange, options }: { value: string; onChange: (value: string) => void; options: { value: string; label: string }[] }) {
  return (
    <select value={value} onChange={(event) => onChange(event.target.value)} className="h-11 w-full rounded border border-stone-400 bg-[#fffdf9] px-3 text-sm text-stone-900 outline-none focus:border-[#b43a2f] focus:ring-2 focus:ring-[#b43a2f]/10">
      {options.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
    </select>
  );
}

function AccountFact({ label, value, className = "", monospace = false }: { label: string; value: string; className?: string; monospace?: boolean }) {
  return (
    <div className={`min-w-0 px-5 py-4 sm:px-8 ${className}`}>
      <p className="text-[10px] font-semibold uppercase tracking-[0.14em] text-stone-500">{label}</p>
      <p className={`mt-1 truncate text-sm text-stone-800 ${monospace ? "font-mono text-xs" : ""}`} title={value}>{value}</p>
    </div>
  );
}

function getInitials(displayName: string): string {
  return displayName.split(/\s+/).filter(Boolean).slice(0, 2).map((part) => part[0]).join("").toUpperCase() || "OB";
}

function describeUserAgent(userAgent: string | null): { title: string; icon: string } {
  if (!userAgent) return { title: "Thiết bị không xác định", icon: "?" };
  const browser = userAgent.includes("Edg/") ? "Microsoft Edge"
    : userAgent.includes("Chrome/") ? "Google Chrome"
      : userAgent.includes("Firefox/") ? "Mozilla Firefox"
        : userAgent.includes("Safari/") ? "Safari"
          : userAgent.includes("PowerShell/") ? "PowerShell"
            : "Trình duyệt khác";
  const platform = userAgent.includes("Windows") ? "Windows"
    : userAgent.includes("Mac OS") ? "macOS"
      : userAgent.includes("Android") ? "Android"
        : /iPhone|iPad/.test(userAgent) ? "iOS"
          : userAgent.includes("Linux") ? "Linux"
            : "thiết bị không xác định";
  return { title: `${browser} · ${platform}`, icon: /Android|iPhone|iPad/.test(userAgent) ? "M" : "D" };
}

function formatRelativeDate(value: string): string {
  const date = new Date(value);
  const delta = Date.now() - date.getTime();
  if (delta >= 0 && delta < 60_000) return "vừa xong";
  if (delta >= 0 && delta < 3_600_000) return `${Math.floor(delta / 60_000)} phút trước`;
  if (delta >= 0 && delta < 86_400_000) return `${Math.floor(delta / 3_600_000)} giờ trước`;
  return date.toLocaleString("vi-VN");
}
