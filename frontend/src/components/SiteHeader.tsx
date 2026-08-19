"use client";

import Link from "next/link";
import { useAuth } from "@/components/AuthProvider";

export function SiteHeader() {
  const { user, hydrated, isAdmin, logout } = useAuth();
  const initials = user?.displayName
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part[0])
    .join("")
    .toUpperCase();

  return (
    <header className="sticky top-0 z-50 border-b border-slate-200/80 bg-white/85 backdrop-blur-xl">
      <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-5 sm:px-8">
        <Link href="/" className="flex items-center gap-3" aria-label="OmniBid home">
          <span className="grid size-9 place-items-center rounded-xl bg-slate-950 text-sm font-black text-white">
            O<span className="text-orange-400">B</span>
          </span>
          <span className="hidden text-xl font-black tracking-tight text-slate-950 sm:inline">
            Omni<span className="text-orange-500">Bid</span>
          </span>
        </Link>
        <nav className="flex items-center gap-1 text-sm font-semibold text-slate-600">
          <Link href="/" className="rounded-lg px-3 py-2 transition hover:bg-slate-100 hover:text-slate-950">
            Đấu giá
          </Link>
          {user && (
            <Link href="/wallet" className="rounded-lg px-3 py-2 transition hover:bg-slate-100 hover:text-slate-950">
              Ví
            </Link>
          )}
          {isAdmin && (
            <span className="hidden rounded-lg bg-violet-50 px-3 py-2 text-violet-700 sm:inline">Admin</span>
          )}
          {!hydrated ? (
            <span className="ml-2 size-9 animate-pulse rounded-full bg-slate-200" />
          ) : user ? (
            <div className="ml-2 flex items-center gap-2">
              <Link
                href="/profile"
                title={user.email}
                className="grid size-9 place-items-center rounded-full bg-orange-100 text-xs font-black text-orange-700 ring-2 ring-white"
              >
                {initials || "ME"}
              </Link>
              <button
                type="button"
                onClick={() => void logout()}
                className="hidden rounded-lg px-2 py-2 text-xs font-bold text-slate-500 hover:bg-slate-100 hover:text-rose-600 sm:block"
              >
                Đăng xuất
              </button>
            </div>
          ) : (
            <Link href="/login" className="ml-2 rounded-xl bg-slate-950 px-4 py-2.5 text-xs font-bold text-white transition hover:bg-orange-500">
              Đăng nhập
            </Link>
          )}
        </nav>
      </div>
    </header>
  );
}
