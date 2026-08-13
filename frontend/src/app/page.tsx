import Link from "next/link";
import { AuctionList } from "@/components/AuctionList";

export default function HomePage() {
  return (
    <main>
      <section className="fine-grid border-b border-slate-200 bg-white">
        <div className="mx-auto max-w-7xl px-5 py-20 sm:px-8 sm:py-28">
          <div className="max-w-3xl">
            <div className="mb-6 inline-flex items-center gap-2 rounded-full border border-orange-200 bg-orange-50 px-4 py-2 text-xs font-bold uppercase tracking-[0.2em] text-orange-700">
              <span className="size-2 animate-pulse rounded-full bg-orange-500" />
              Distributed auction lab
            </div>
            <h1 className="text-5xl font-black leading-[0.98] tracking-[-0.04em] text-slate-950 sm:text-7xl">
              Sở hữu món đồ hiếm.
              <span className="mt-2 block text-orange-500">Đặt giá trong tích tắc.</span>
            </h1>
            <p className="mt-7 max-w-2xl text-lg leading-8 text-slate-600">
              Trải nghiệm đấu giá real-time với Redis Distributed Lock, ví gRPC và kiến trúc event-driven được thiết kế cho tải đồng thời cao.
            </p>
            <div className="mt-8 flex flex-wrap items-center gap-4">
              <a href="#live-auctions" className="rounded-xl bg-slate-950 px-6 py-3.5 text-sm font-bold text-white transition hover:bg-orange-500">
                Xem phiên đang mở
              </a>
              <Link href="/wallet" className="rounded-xl border border-slate-300 bg-white px-6 py-3.5 text-sm font-bold text-slate-800 transition hover:border-slate-950">
                Kiểm tra ví demo
              </Link>
            </div>
          </div>
        </div>
      </section>

      <section id="live-auctions" className="mx-auto max-w-7xl px-5 py-16 sm:px-8 sm:py-20">
        <div className="mb-9 flex flex-col justify-between gap-3 sm:flex-row sm:items-end">
          <div>
            <p className="text-xs font-bold uppercase tracking-[0.2em] text-orange-600">Live marketplace</p>
            <h2 className="mt-2 text-3xl font-black tracking-tight text-slate-950">Phiên đấu giá nổi bật</h2>
          </div>
          <p className="flex items-center gap-2 text-sm text-slate-500">
            <span className="size-2 animate-pulse rounded-full bg-emerald-500" />
            Dữ liệu tự cập nhật mỗi 3 giây
          </p>
        </div>
        <AuctionList />
      </section>
    </main>
  );
}
