"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useState } from "react";
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
        <Skeleton className="h-[520px]" />
        <Skeleton className="h-[520px]" />
      </div>
    );
  }

  if (error && auctions.length === 0) {
    return <div className="border border-red-300 bg-red-50 p-6 text-red-800">{error}</div>;
  }

  return (
    <div className="grid gap-x-7 gap-y-12 md:grid-cols-2">
      {auctions.map((auction, index) => (
        <Link href={`/auctions/${auction.id}`} key={auction.id} className="group block">
          <article>
            <div className="relative overflow-hidden border border-stone-300 bg-stone-200">
              <Image
                src="/keyboard.svg"
                alt={auction.title}
                width={1200}
                height={900}
                className="aspect-[4/3] w-full object-cover transition duration-500 group-hover:scale-[1.015]"
              />
              <div className="absolute inset-x-0 top-0 flex items-center justify-between p-4 text-[10px] font-semibold uppercase tracking-[0.16em]">
                <span className="bg-[#fffdf9] px-2.5 py-1.5 text-stone-700">Lô {String(index + 1).padStart(2, "0")}</span>
                <span className="bg-[#262521] px-2.5 py-1.5 text-white">{auction.status === "ACTIVE" ? "Đang mở" : "Đã đóng"}</span>
              </div>
            </div>
            <div className="pt-5">
              <div className="flex items-start justify-between gap-4">
                <div>
                  <p className="mb-2 text-[10px] font-semibold uppercase tracking-[0.18em] text-[#b43a2f]">Thiết kế & công nghệ</p>
                  <h2 className="display-serif text-2xl leading-tight text-stone-950 transition group-hover:text-[#b43a2f] sm:text-3xl">{auction.title}</h2>
                </div>
                <span className="mt-1 text-xl text-stone-400 transition group-hover:translate-x-1 group-hover:text-[#b43a2f]">↗</span>
              </div>
              <div className="mt-5 grid grid-cols-2 border-y border-stone-300 py-4">
                <div>
                  <p className="text-[10px] uppercase tracking-[0.12em] text-stone-500">Giá hiện tại</p>
                  <p className="mt-1 text-lg font-semibold tabular-nums text-stone-950">{formatMoney(Number(auction.currentPrice))}</p>
                </div>
                <div className="border-l border-stone-300 pl-5">
                  <p className="text-[10px] uppercase tracking-[0.12em] text-stone-500">Kết thúc</p>
                  <p className="mt-1 text-sm font-medium text-stone-800">{new Date(auction.endTime).toLocaleString("vi-VN", { day: "2-digit", month: "2-digit", hour: "2-digit", minute: "2-digit" })}</p>
                </div>
              </div>
            </div>
          </article>
        </Link>
      ))}
    </div>
  );
}
