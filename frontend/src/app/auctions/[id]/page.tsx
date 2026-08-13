"use client";

import Image from "next/image";
import Link from "next/link";
import { use, useCallback, useEffect, useMemo, useRef, useState, type FormEvent } from "react";
import { CountdownTimer } from "@/components/CountdownTimer";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { getApiErrorMessage } from "@/lib/errors";
import { anonymizeUserId, formatMoney } from "@/lib/utils";
import { getAuctionDetail, getBidHistory, placeBid } from "@/services/auctionService";
import type { Auction, BidHistory } from "@/types/auction";

const DEMO_USERS = [
  { id: "22222222-2222-2222-2222-222222222222", label: "Người dùng A" },
  { id: "33333333-3333-3333-3333-333333333333", label: "Người dùng B" }
];

export default function AuctionDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const [auction, setAuction] = useState<Auction | null>(null);
  const [bids, setBids] = useState<BidHistory[]>([]);
  const [userId, setUserId] = useState(DEMO_USERS[0].id);
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

    const numericAmount = Number(amount);
    if (!Number.isFinite(numericAmount) || numericAmount < suggestedBid) {
      setFeedback({ type: "error", text: `Giá tối thiểu là ${formatMoney(suggestedBid)}.` });
      return;
    }

    setSubmitting(true);
    setFeedback(null);
    try {
      await placeBid(auction.id, userId, numericAmount);
      amountTouched.current = false;
      setFeedback({ type: "success", text: "Đặt giá thành công. Kafka audit event đã được phát." });
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
        <p className="rounded-3xl border border-rose-200 bg-rose-50 p-8 font-semibold text-rose-700">{error || "Không tìm thấy phiên đấu giá."}</p>
        <Link href="/" className="mt-6 inline-block font-bold text-orange-600">← Trở về danh sách</Link>
      </main>
    );
  }

  const isActive = auction.status === "ACTIVE" && new Date(auction.endTime).getTime() > Date.now();

  return (
    <main className="mx-auto max-w-7xl px-5 py-8 sm:px-8 sm:py-12">
      <div className="mb-7 flex flex-wrap items-center justify-between gap-3">
        <Link href="/" className="text-sm font-bold text-slate-500 transition hover:text-orange-600">← Tất cả phiên đấu giá</Link>
        <div className="flex items-center gap-2 text-xs font-medium text-slate-500">
          <span className="size-2 animate-pulse rounded-full bg-emerald-500" />
          Live polling 1.5 giây · phiên bản {auction.version}
        </div>
      </div>

      {error && <div className="mb-6 rounded-2xl border border-amber-200 bg-amber-50 px-5 py-4 text-sm text-amber-800">Mất kết nối tạm thời: {error}</div>}

      <div className="grid gap-8 lg:grid-cols-[1.08fr_0.92fr]">
        <section className="space-y-7">
          <div className="relative overflow-hidden rounded-[2rem] border border-slate-200 bg-slate-100">
            <div className="absolute left-5 top-5 z-10 flex gap-2">
              <Badge className="border-white/70 bg-white/90 text-slate-700">Hàng sưu tầm</Badge>
              <Badge className="border-orange-200 bg-orange-50/90 text-orange-700">Founder&apos;s Edition</Badge>
            </div>
            <Image
              src="/keyboard.svg"
              alt="Bàn phím cơ phiên bản giới hạn"
              width={1200}
              height={900}
              priority
              className="aspect-[4/3] w-full object-cover transition duration-700 hover:scale-[1.02]"
            />
          </div>

          <div>
            <p className="text-xs font-bold uppercase tracking-[0.2em] text-orange-600">Lot #OMNI-2026-01</p>
            <h1 className="mt-3 text-3xl font-black tracking-tight text-slate-950 sm:text-5xl">{auction.title}</h1>
            <p className="mt-5 max-w-3xl text-base leading-8 text-slate-600">
              Bàn phím cơ phiên bản giới hạn với khung nhôm CNC, switch linear được tinh chỉnh thủ công và keycap PBT double-shot. Mỗi sản phẩm có số serial riêng dành cho nhà sưu tầm.
            </p>
          </div>

          <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
            {[
              ["Giá khởi điểm", formatMoney(Number(auction.startingPrice))],
              ["Bước giá", formatMoney(Number(auction.stepPrice))],
              ["Tiền cọc", formatMoney(Number(auction.depositAmount))],
              ["Lượt đặt giá", String(bids.length)]
            ].map(([label, value]) => (
              <div key={label} className="rounded-2xl border border-slate-200 bg-white p-4">
                <p className="text-xs font-medium text-slate-500">{label}</p>
                <p className="mt-1 font-black text-slate-950">{value}</p>
              </div>
            ))}
          </div>
        </section>

        <aside className="space-y-6">
          <Card className="overflow-hidden !border-slate-900 !bg-slate-950 !text-white shadow-2xl">
            <CardContent className="p-6 sm:p-8">
              <div className="mb-6 flex items-center justify-between">
                <div>
                  <p className="text-xs font-bold uppercase tracking-[0.2em] text-slate-400">Trạng thái phiên</p>
                  <p className="mt-1 flex items-center gap-2 font-bold text-white">
                    <span className={`size-2 rounded-full ${isActive ? "animate-pulse bg-emerald-400" : "bg-rose-400"}`} />
                    {isActive ? "Đang nhận giá" : "Đã đóng"}
                  </p>
                </div>
                <Badge className="!border-white/10 !bg-white/5 !text-slate-300">{auction.status}</Badge>
              </div>

              <CountdownTimer endTime={auction.endTime} />

              <div className="my-7 border-y border-white/10 py-7">
                <p className="text-sm font-medium text-slate-400">Giá cao nhất hiện tại</p>
                <p key={pricePulse} className="animate-price-pop mt-2 text-4xl font-black tracking-tight text-white sm:text-5xl">
                  {formatMoney(Number(auction.currentPrice))}
                </p>
                {auction.winningUserId && (
                  <p className="mt-3 text-xs text-slate-500">Dẫn đầu bởi {anonymizeUserId(auction.winningUserId)}</p>
                )}
              </div>

              <form onSubmit={submitBid} className="space-y-4">
                <div>
                  <label htmlFor="bidder" className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-400">Tài khoản demo</label>
                  <select
                    id="bidder"
                    value={userId}
                    onChange={(event) => setUserId(event.target.value)}
                    className="h-12 w-full rounded-xl border border-white/10 bg-white/5 px-4 text-sm text-white outline-none focus:border-orange-400"
                  >
                    {DEMO_USERS.map((user) => <option key={user.id} value={user.id} className="bg-slate-900">{user.label} · {anonymizeUserId(user.id)}</option>)}
                  </select>
                </div>
                <div>
                  <div className="mb-2 flex items-center justify-between">
                    <label htmlFor="amount" className="text-xs font-bold uppercase tracking-[0.14em] text-slate-400">Giá của bạn</label>
                    <button
                      type="button"
                      onClick={() => {
                        amountTouched.current = false;
                        setAmount(String(suggestedBid));
                      }}
                      className="text-xs font-bold text-orange-400 hover:text-orange-300"
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
                      className="h-14 !border-white/10 !bg-white/5 pr-16 text-xl font-black !text-white focus:!border-orange-400 focus:!ring-orange-500/10"
                    />
                    <span className="absolute right-4 top-1/2 -translate-y-1/2 text-xs font-bold text-slate-500">VND</span>
                  </div>
                  <p className="mt-2 text-xs text-slate-500">Tối thiểu {formatMoney(suggestedBid)} · Header idempotency tự động</p>
                </div>

                <Button disabled={!isActive || submitting} className="h-14 w-full text-base">
                  {submitting ? (
                    <><span className="size-4 animate-spin rounded-full border-2 border-white/30 border-t-white" /> Đang lấy Redis Lock...</>
                  ) : (
                    <>Bấm đặt giá <span aria-hidden>→</span></>
                  )}
                </Button>

                {feedback && (
                  <p className={`rounded-xl px-4 py-3 text-sm ${feedback.type === "success" ? "bg-emerald-400/10 text-emerald-300" : "bg-rose-400/10 text-rose-300"}`}>
                    {feedback.text}
                  </p>
                )}
              </form>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-0">
              <div className="flex items-center justify-between border-b border-slate-100 px-6 py-5">
                <div>
                  <h2 className="font-black text-slate-950">Lịch sử đặt giá</h2>
                  <p className="mt-1 text-xs text-slate-500">Cập nhật tự động qua polling</p>
                </div>
                <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-bold text-slate-600">{bids.length} bid</span>
              </div>
              <div className="max-h-80 overflow-y-auto">
                {bids.length === 0 ? (
                  <p className="px-6 py-12 text-center text-sm text-slate-500">Chưa có lượt đặt giá. Hãy là người đầu tiên.</p>
                ) : bids.map((bid, index) => (
                  <div key={bid.bidId} className="flex items-center gap-4 border-b border-slate-100 px-6 py-4 last:border-0">
                    <span className={`grid size-10 shrink-0 place-items-center rounded-full text-xs font-black ${index === 0 ? "bg-orange-100 text-orange-700" : "bg-slate-100 text-slate-600"}`}>
                      {bid.userId.slice(0, 2).toUpperCase()}
                    </span>
                    <div className="min-w-0 flex-1">
                      <p className="truncate font-mono text-xs font-bold text-slate-700">{anonymizeUserId(bid.userId)}</p>
                      <p className="mt-1 text-xs text-slate-400">{new Date(bid.placedAt).toLocaleString("vi-VN")}</p>
                    </div>
                    <div className="text-right">
                      <p className="font-black text-slate-950">{formatMoney(Number(bid.amount))}</p>
                      {index === 0 && <p className="mt-1 text-[10px] font-bold uppercase tracking-wider text-orange-600">Cao nhất</p>}
                    </div>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        </aside>
      </div>
    </main>
  );
}
