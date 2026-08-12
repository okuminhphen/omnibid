import { AuctionList } from "@/components/AuctionList";

export default function HomePage() {
  return (
    <main className="mx-auto min-h-screen max-w-6xl px-6 py-16">
      <div className="mb-12 flex flex-col gap-4">
        <span className="w-fit rounded-full border border-orange-400/30 bg-orange-400/10 px-3 py-1 text-xs font-semibold uppercase tracking-[0.2em] text-orange-300">
          Distributed auction lab
        </span>
        <h1 className="text-5xl font-black tracking-tight text-white sm:text-7xl">
          Omni<span className="text-signal">Bid</span>
        </h1>
        <p className="max-w-2xl text-lg text-slate-400">
          Đấu giá real-time với Redis distributed lock, gRPC wallet và event-driven audit.
        </p>
      </div>
      <AuctionList />
    </main>
  );
}
