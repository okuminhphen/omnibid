"use client";

import Image from "next/image";
import Link from "next/link";
import { use, useCallback, useEffect, useMemo, useRef, useState, type FormEvent } from "react";
import { useAuth } from "@/components/AuthProvider";
import { CountdownTimer } from "@/components/CountdownTimer";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { getApiErrorMessage } from "@/lib/errors";
import { anonymizeUserId, formatMoney } from "@/lib/utils";
import { getAuctionDetail, getBidHistory, placeBid } from "@/services/auctionService";
import type { Auction, BidHistory } from "@/types/auction";

export default function AuctionDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const { user, hydrated } = useAuth();
  const [auction, setAuction] = useState<Auction | null>(null);
  const [bids, setBids] = useState<BidHistory[]>([]);
  const [amount, setAmount] = useState("");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [feedback, setFeedback] = useState<{ type: "success" | "error"; text: string } | null>(null);
  const [pricePulse, setPricePulse] = useState(0);
  const lastPrice = useRef<number | null>(null);
  const amountTouched = useRef(false);

  const refresh = useCallback(async (showLoading = false) => {
    if (showLoading) setLoading(true);
    try {
      const [nextAuction, nextBids] = await Promise.all([
        getAuctionDetail(id),
        getBidHistory(id)
      ]);
      const numericPrice = Number(nextAuction.currentPrice);
      if (lastPrice.current !== null && numericPrice !== lastPrice.current) {
        setPricePulse((value) => value + 1);
      }
      lastPrice.current = numericPrice;
      setAuction(nextAuction);
      setBids(nextBids);
      setError("");
      if (!amountTouched.current) {
        setAmount(String(numericPrice + Number(nextAuction.stepPrice)));
      }
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, "Không thể tải dữ liệu phiên đấu giá."));
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    let active = true;
    const poll = async () => {
      if (active) await refresh();
    };
    void refresh(true);
    const timer = window.setInterval(poll, 1_500);
    return () => {
      active = false;
      window.clearInterval(timer);
    };
  }, [refresh]);

  const suggestedBid = useMemo(() => {
    if (!auction) return 0;
    return Number(auction.currentPrice) + Number(auction.stepPrice);
  }, [auction]);

  async function submitBid(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!auction) return;
    if (!user) {
      setFeedback({ type: "error", text: "Bạn cần đăng nhập trước khi đặt giá." });
      return;
    }

    const numericAmount = Number(amount);
    if (!Number.isFinite(numericAmount) || numericAmount < suggestedBid) {
      setFeedback({ type: "error", text: `Giá tối thiểu là ${formatMoney(suggestedBid)}.` });
      return;
    }

    setSubmitting(true);
    setFeedback(null);
    try {
      await placeBid(auction.id, numericAmount);
      amountTouched.current = false;
      setFeedback({ type: "success", text: "Giá của bạn đã được ghi nhận." });
      await refresh();
    } catch (requestError) {
      setFeedback({
        type: "error",
        text: getApiErrorMessage(requestError, "Lượt đặt giá bị từ chối.")
      });
    } finally {
      setSubmitting(false);
    }
  }

  if (loading && !auction) {
    return (
      <main className="mx-auto max-w-7xl px-5 py-10 sm:px-8">
        <Skeleton className="mb-6 h-8 w-48" />
        <div className="grid gap-8 lg:grid-cols-[1.08fr_0.92fr]">
          <Skeleton className="h-[620px]" />
          <Skeleton className="h-[620px]" />
        </div>
      </main>
    );
  }

  if (!auction) {
    return (
      <main className="mx-auto max-w-3xl px-5 py-20 text-center">
        <p className="rounded-sm border border-rose-200 bg-rose-50 p-8 font-semibold text-rose-700">{error || "Không tìm thấy phiên đấu giá."}</p>
        <Link href="/" className="mt-6 inline-block font-bold text-orange-600">← Trở về danh sách</Link>
      </main>
    );
  }

  const isActive = auction.status === "ACTIVE" && new Date(auction.endTime).getTime() > Date.now();

  return (
    <main className="mx-auto max-w-[1440px] px-5 py-8 sm:px-8 sm:py-12 lg:px-12">
      <div className="mb-8 flex flex-wrap items-center justify-between gap-3 border-b border-stone-300 pb-5">
        <Link href="/" className="text-sm text-stone-500 transition hover:text-[#b43a2f]">← Trở lại danh sách</Link>
        <div className="flex items-center gap-2 text-[10px] font-semibold uppercase tracking-[0.14em] text-stone-500">
          <span className="size-1.5 rounded-full bg-[#b43a2f]" />
          Giá đang được cập nhật · #{auction.version}
        </div>
      </div>

      {error && <div className="mb-6 border border-amber-300 bg-amber-50 px-5 py-4 text-sm text-amber-900">Mất kết nối tạm thời: {error}</div>}

      <div className="grid gap-10 lg:grid-cols-[1.18fr_0.82fr] lg:gap-14">
        <section>
          <div className="relative overflow-hidden border border-stone-300 bg-stone-200">
            <span className="absolute left-4 top-4 z-10 bg-[#fffdf9] px-3 py-2 text-[10px] font-semibold uppercase tracking-[0.16em] text-stone-700">Lô 01 · Phiên giới hạn</span>
            <Image
              src="/keyboard.svg"
              alt="Bàn phím cơ phiên bản giới hạn"
              width={1200}
              height={900}
              priority
              className="aspect-[4/3] w-full object-cover"
            />
          </div>

          <div className="border-b border-stone-300 py-8">
            <p className="text-[10px] font-semibold uppercase tracking-[0.2em] text-[#b43a2f]">Thiết kế & công nghệ · OMNI-2026-01</p>
            <h1 className="display-serif mt-3 text-4xl leading-[1.05] tracking-[-0.035em] text-stone-950 sm:text-6xl">{auction.title}</h1>
            <p className="mt-6 max-w-3xl text-base leading-8 text-stone-600">
              Bàn phím cơ phiên bản giới hạn với khung nhôm CNC, switch linear được tinh chỉnh thủ công và keycap PBT double-shot. Mỗi sản phẩm có số serial riêng dành cho nhà sưu tầm.
            </p>
          </div>

          <div className="grid grid-cols-2 border-b border-stone-300 sm:grid-cols-4">
            {[
              ["Giá khởi điểm", formatMoney(Number(auction.startingPrice))],
              ["Bước giá", formatMoney(Number(auction.stepPrice))],
              ["Tiền cọc", formatMoney(Number(auction.depositAmount))],
              ["Lượt đặt giá", String(bids.length)]
            ].map(([label, value]) => (
              <div key={label} className="border-r border-t border-stone-300 px-3 py-5 last:border-r-0 sm:first:border-l">
                <p className="text-[10px] uppercase tracking-[0.12em] text-stone-500">{label}</p>
                <p className="mt-2 text-sm font-semibold tabular-nums text-stone-950">{value}</p>
              </div>
            ))}
          </div>
        </section>

        <aside className="space-y-8 lg:sticky lg:top-24 lg:self-start">
          <section className="border border-stone-400 bg-[#fffdf9] p-6 sm:p-8">
              <div className="mb-7 flex items-center justify-between border-b border-stone-300 pb-4">
                <p className="flex items-center gap-2 text-sm font-medium text-stone-800">
                  <span className={`size-1.5 rounded-full ${isActive ? "bg-[#b43a2f]" : "bg-stone-400"}`} />
                  {isActive ? "Đang nhận giá" : "Đã đóng"}
                </p>
                <span className="text-[10px] font-semibold uppercase tracking-[0.14em] text-stone-500">{auction.status}</span>
              </div>

              <p className="mb-3 text-[10px] font-semibold uppercase tracking-[0.16em] text-stone-500">Thời gian còn lại</p>
              <CountdownTimer endTime={auction.endTime} />

              <div className="my-8 border-y border-stone-300 py-7">
                <p className="text-[10px] font-semibold uppercase tracking-[0.16em] text-stone-500">Giá cao nhất hiện tại</p>
                <p key={pricePulse} className="animate-price-pop display-serif mt-2 text-4xl tracking-[-0.03em] text-stone-950 sm:text-5xl">
                  {formatMoney(Number(auction.currentPrice))}
                </p>
                {auction.winningUserId && (
                  <p className="mt-3 text-xs text-stone-500">Dẫn đầu bởi {anonymizeUserId(auction.winningUserId)}</p>
                )}
              </div>

              <form onSubmit={submitBid} className="space-y-4">
                {user ? (
                  <div className="flex items-center justify-between border-b border-stone-300 pb-4">
                    <div>
                      <p className="text-[9px] font-semibold uppercase tracking-[0.14em] text-stone-500">Đặt giá với</p>
                      <p className="mt-1 text-sm font-semibold text-stone-950">{user.displayName}</p>
                    </div>
                    <span className="font-mono text-xs text-stone-400">{anonymizeUserId(user.id)}</span>
                  </div>
                ) : hydrated ? (
                  <Link href="/login" className="block border border-[#b43a2f] px-4 py-3 text-center text-sm font-semibold text-[#b43a2f] hover:bg-red-50">
                    Đăng nhập để đặt giá
                  </Link>
                ) : (
                  <div className="h-12 animate-pulse bg-stone-200" />
                )}
                <div>
                  <div className="mb-2 flex items-center justify-between">
                    <label htmlFor="amount" className="text-[10px] font-semibold uppercase tracking-[0.14em] text-stone-500">Giá của bạn</label>
                    <button
                      type="button"
                      onClick={() => {
                        amountTouched.current = false;
                        setAmount(String(suggestedBid));
                      }}
                      className="text-xs font-medium text-[#b43a2f] hover:underline"
                    >
                      Dùng giá gợi ý
                    </button>
                  </div>
                  <div className="relative">
                    <Input
                      id="amount"
                      type="number"
                      min={suggestedBid}
                      step={Number(auction.stepPrice)}
                      value={amount}
                      onChange={(event) => {
                        amountTouched.current = true;
                        setAmount(event.target.value);
                      }}
                      disabled={!isActive || submitting}
                      className="h-14 pr-16 text-xl font-semibold tabular-nums"
                    />
                    <span className="absolute right-4 top-1/2 -translate-y-1/2 text-xs font-semibold text-stone-500">VND</span>
                  </div>
                  <p className="mt-2 text-xs text-stone-500">Mức tối thiểu {formatMoney(suggestedBid)}</p>
                </div>

                <Button disabled={!isActive || submitting || !user} className="h-14 w-full text-base">
                  {submitting ? (
                    <><span className="size-4 animate-spin rounded-full border-2 border-white/30 border-t-white" /> Đang gửi...</>
                  ) : (
                    <>Xác nhận đặt giá <span aria-hidden>→</span></>
                  )}
                </Button>

                {feedback && (
                  <p className={`border px-4 py-3 text-sm ${feedback.type === "success" ? "border-emerald-300 bg-emerald-50 text-emerald-800" : "border-red-300 bg-red-50 text-red-800"}`}>
                    {feedback.text}
                  </p>
                )}
              </form>
          </section>

          <section className="border-t border-stone-400">
              <div className="flex items-center justify-between border-b border-stone-300 py-5">
                <div>
                  <h2 className="display-serif text-2xl text-stone-950">Lịch sử đặt giá</h2>
                  <p className="mt-1 text-xs text-stone-500">Các lượt gần nhất</p>
                </div>
                <span className="text-xs font-semibold text-stone-500">{bids.length} lượt</span>
              </div>
              <div className="max-h-80 overflow-y-auto">
                {bids.length === 0 ? (
                  <p className="py-12 text-center text-sm text-stone-500">Chưa có lượt đặt giá.</p>
                ) : bids.map((bid, index) => (
                  <div key={bid.bidId} className="grid grid-cols-[1fr_auto] items-center gap-4 border-b border-stone-300 py-4 last:border-0">
                    <div className="min-w-0 flex-1">
                      <p className="truncate font-mono text-xs font-semibold text-stone-700">{anonymizeUserId(bid.userId)} {index === 0 && <span className="ml-2 text-[9px] uppercase tracking-[0.12em] text-[#b43a2f]">Dẫn đầu</span>}</p>
                      <p className="mt-1 text-xs text-stone-400">{new Date(bid.placedAt).toLocaleString("vi-VN")}</p>
                    </div>
                    <div className="text-right">
                      <p className="font-semibold tabular-nums text-stone-950">{formatMoney(Number(bid.amount))}</p>
                    </div>
                  </div>
                ))}
              </div>
          </section>
        </aside>
      </div>
    </main>
  );
}
