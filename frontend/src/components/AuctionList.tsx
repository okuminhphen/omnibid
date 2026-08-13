"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { Badge } from "@/components/ui/badge";
import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { getApiErrorMessage } from "@/lib/errors";
import { formatMoney } from "@/lib/utils";
import { listAuctions } from "@/services/auctionService";
import type { Auction } from "@/types/auction";

export function AuctionList() {
  const [auctions, setAuctions] = useState<Auction[]>([]);
  const [loading, setLoading] = useState(true);
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
      } catch (requestError) {
        if (active) {
          setError(getApiErrorMessage(requestError, "Không kết nối được auction-service."));
        }
      } finally {
        if (active) setLoading(false);
      }
    };

    void refresh();
    const timer = window.setInterval(refresh, 3_000);
    return () => {
      active = false;
      window.clearInterval(timer);
    };
  }, []);

  if (loading) {
    return (
      <div className="grid gap-6 md:grid-cols-2">
        <Skeleton className="h-72" />
        <Skeleton className="h-72" />
      </div>
    );
  }

  if (error && auctions.length === 0) {
    return <div className="rounded-3xl border border-rose-200 bg-rose-50 p-6 text-rose-700">{error}</div>;
  }

  return (
    <div className="grid gap-6 md:grid-cols-2">
      {auctions.map((auction, index) => (
        <Link href={`/auctions/${auction.id}`} key={auction.id} className="group">
          <Card className="h-full overflow-hidden transition duration-300 hover:-translate-y-1 hover:border-orange-200 hover:shadow-xl">
            <div className="relative h-44 overflow-hidden bg-slate-950 p-6">
              <div className="absolute -right-12 -top-16 size-48 rounded-full bg-orange-500/25 blur-3xl" />
              <div className="absolute inset-x-8 bottom-6 h-14 rotate-[-4deg] rounded-2xl border border-white/10 bg-gradient-to-r from-slate-800 to-slate-700 shadow-2xl">
                <div className="grid h-full grid-cols-10 gap-1 p-2 opacity-80">
                  {Array.from({ length: 30 }).map((_, key) => (
                    <span key={key} className="rounded-[3px] bg-slate-500/50" />
                  ))}
                </div>
              </div>
              <Badge className={auction.status === "ACTIVE" ? "!border-emerald-400/20 !bg-emerald-400/10 !text-emerald-300" : "!border-white/10 !bg-white/10 !text-slate-300"}>
                {auction.status === "ACTIVE" && <span className="mr-2 size-1.5 animate-pulse rounded-full bg-emerald-400" />}
                {auction.status}
              </Badge>
              <span className="absolute right-5 top-5 font-mono text-xs text-slate-500">LOT 0{index + 1}</span>
            </div>
            <div className="p-6">
              <p className="mb-2 text-xs font-bold uppercase tracking-[0.18em] text-orange-600">Sưu tầm công nghệ</p>
              <h2 className="min-h-14 text-xl font-black text-slate-950 transition group-hover:text-orange-600">{auction.title}</h2>
              <div className="mt-6 flex items-end justify-between border-t border-slate-100 pt-5">
                <div>
                  <p className="text-xs font-medium text-slate-500">Giá hiện tại</p>
                  <p className="mt-1 text-2xl font-black text-slate-950">{formatMoney(Number(auction.currentPrice))}</p>
                </div>
                <span className="grid size-11 place-items-center rounded-full bg-slate-950 text-xl text-white transition group-hover:bg-orange-500">→</span>
              </div>
            </div>
          </Card>
        </Link>
      ))}
    </div>
  );
}
