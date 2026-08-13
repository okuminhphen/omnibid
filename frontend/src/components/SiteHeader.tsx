import Link from "next/link";

export function SiteHeader() {
  return (
    <header className="sticky top-0 z-50 border-b border-slate-200/80 bg-white/85 backdrop-blur-xl">
      <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-5 sm:px-8">
        <Link href="/" className="flex items-center gap-3" aria-label="OmniBid home">
          <span className="grid size-9 place-items-center rounded-xl bg-slate-950 text-sm font-black text-white">
            O<span className="text-orange-400">B</span>
          </span>
          <span className="text-xl font-black tracking-tight text-slate-950">
            Omni<span className="text-orange-500">Bid</span>
          </span>
        </Link>
        <nav className="flex items-center gap-1 text-sm font-semibold text-slate-600">
          <Link href="/" className="rounded-lg px-3 py-2 transition hover:bg-slate-100 hover:text-slate-950">
            Phiên đấu giá
          </Link>
          <Link href="/wallet" className="rounded-lg px-3 py-2 transition hover:bg-slate-100 hover:text-slate-950">
            Ví của tôi
          </Link>
        </nav>
      </div>
    </header>
  );
}
