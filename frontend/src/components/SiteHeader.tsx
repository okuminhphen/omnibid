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
    <header className="sticky top-0 z-50 border-b border-stone-300 bg-[#f4f1ea]/95 backdrop-blur-md">
      <div className="mx-auto flex h-[72px] max-w-[1440px] items-center justify-between px-5 sm:px-8 lg:px-12">
        <Link href="/" className="flex items-baseline gap-3" aria-label="OmniBid home">
          <span className="display-serif text-2xl font-bold tracking-[-0.04em] text-stone-950">OmniBid</span>
          <span className="hidden text-[9px] font-semibold uppercase tracking-[0.2em] text-stone-500 sm:inline">Auction house</span>
        </Link>
        <nav className="flex items-center gap-1 text-sm font-medium text-stone-600 sm:gap-2">
          <Link href="/" className="px-3 py-2 transition hover:text-stone-950">
            Phiên đấu giá
          </Link>
          {user && (
            <Link href="/wallet" className="px-3 py-2 transition hover:text-stone-950">
              Ví
            </Link>
          )}
          {isAdmin && (
            <span className="hidden border-l border-stone-300 px-3 py-1 text-[10px] font-semibold uppercase tracking-[0.14em] text-[#b43a2f] sm:inline">Quản trị</span>
          )}
          {!hydrated ? (
            <span className="ml-2 size-9 animate-pulse rounded bg-stone-300" />
          ) : user ? (
            <div className="ml-2 flex items-center gap-2">
              <Link
                href="/profile"
                title={user.email}
                className="grid size-9 place-items-center rounded border border-stone-400 bg-[#fffdf9] text-xs font-semibold text-stone-800"
              >
                {initials || "ME"}
              </Link>
              <button
                type="button"
                onClick={() => void logout()}
                className="hidden px-2 py-2 text-xs font-medium text-stone-500 hover:text-[#b43a2f] sm:block"
              >
                Đăng xuất
              </button>
            </div>
          ) : (
            <Link href="/login" className="ml-2 rounded border border-stone-950 bg-stone-950 px-4 py-2.5 text-xs font-semibold text-white transition hover:border-[#b43a2f] hover:bg-[#b43a2f]">
              Đăng nhập
            </Link>
          )}
        </nav>
      </div>
    </header>
  );
}
