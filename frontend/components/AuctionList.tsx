"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { listAuctions } from "@/services/api";
import type { Auction } from "@/types/auction";

export function AuctionList() {
  const [auctions, setAuctions] = useState<Auction[]>([]);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    const refresh = async () => {
      try {
        const data = await listAuctions();
        if (active) {
          setAuctions(data);
          setError("");
        }
      } catch {
        if (active) setError("Không kết nối được auction-service tại port 8080.");
      }
    };
    void refresh();
    const timer = window.setInterval(refresh, 2_000);
    return () => {
      active = false;
      window.clearInterval(timer);
    };
  }, []);

  if (error) {
    return <div className="rounded-2xl border border-red-400/30 bg-red-400/10 p-6 text-red-200">{error}</div>;
  }

  return (
    <div className="grid gap-5 md:grid-cols-2">
      {auctions.map((auction) => (
        <Link
          href={`/auctions/${auction.id}`}
          key={auction.id}
          className="group rounded-3xl border border-slate-800 bg-slate-900/70 p-7 transition hover:-translate-y-1 hover:border-orange-400/50"
        >
          <div className="mb-10 flex items-center justify-between">
            <span className="rounded-full bg-emerald-400/10 px-3 py-1 text-xs font-bold text-emerald-300">
              {auction.status}
            </span>
            <span className="font-mono text-xs text-slate-500">v{auction.version}</span>
          </div>
          <h2 className="mb-3 text-2xl font-bold text-white group-hover:text-orange-300">{auction.title}</h2>
          <p className="text-sm text-slate-500">Giá hiện tại</p>
          <p className="mt-1 text-3xl font-black text-white">
            ${Number(auction.currentPrice).toLocaleString("en-US")}
          </p>
        </Link>
      ))}
    </div>
  );
}
