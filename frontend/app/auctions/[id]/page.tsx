"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { BidPanel } from "@/components/BidPanel";
import { getAuction } from "@/services/api";
import type { Auction } from "@/types/auction";

export default function AuctionDetailPage({ params }: { params: { id: string } }) {
  const [auction, setAuction] = useState<Auction | null>(null);
  const [error, setError] = useState("");

  const refresh = useCallback(async () => {
    try {
      setAuction(await getAuction(params.id));
      setError("");
    } catch {
      setError("Không tải được phiên đấu giá.");
    }
  }, [params.id]);

  useEffect(() => {
    void refresh();
    const timer = window.setInterval(refresh, 2_000);
    return () => window.clearInterval(timer);
  }, [refresh]);

  return (
    <main className="mx-auto min-h-screen max-w-4xl px-6 py-14">
      <Link href="/" className="mb-10 inline-block text-sm font-bold text-orange-300">← All auctions</Link>
      {error && <p className="rounded-xl bg-red-400/10 p-5 text-red-200">{error}</p>}
      {auction && (
        <div className="grid gap-8 md:grid-cols-[1fr_22rem]">
          <section>
            <span className="text-sm font-bold text-emerald-300">{auction.status}</span>
            <h1 className="mt-3 text-4xl font-black text-white">{auction.title}</h1>
            <p className="mt-10 text-sm text-slate-500">Current highest bid</p>
            <p className="mt-1 text-6xl font-black text-white">
              ${Number(auction.currentPrice).toLocaleString("en-US")}
            </p>
            <dl className="mt-10 space-y-3 text-sm text-slate-400">
              <div><dt className="inline font-semibold text-slate-200">Auction ID: </dt><dd className="inline font-mono">{auction.id}</dd></div>
              <div><dt className="inline font-semibold text-slate-200">Ends: </dt><dd className="inline">{new Date(auction.endsAt).toLocaleString()}</dd></div>
              <div><dt className="inline font-semibold text-slate-200">Version: </dt><dd className="inline">{auction.version}</dd></div>
            </dl>
          </section>
          <BidPanel auctionId={auction.id} currentPrice={Number(auction.currentPrice)} onPlaced={refresh} />
        </div>
      )}
    </main>
  );
}
