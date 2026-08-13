"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { getApiErrorMessage } from "@/lib/errors";
import { anonymizeUserId, formatMoney } from "@/lib/utils";
import { getWallet, topUpWallet } from "@/services/walletService";
import type { WalletInfo } from "@/types/auction";

const DEMO_USERS = [
  { id: "22222222-2222-2222-2222-222222222222", label: "Người dùng A" },
  { id: "33333333-3333-3333-3333-333333333333", label: "Người dùng B" }
];
const DEMO_TOP_UP = 500_000;

export default function WalletPage() {
  const [userId, setUserId] = useState(DEMO_USERS[0].id);
  const [wallet, setWallet] = useState<WalletInfo | null>(null);
  const [loading, setLoading] = useState(true);
  const [toppingUp, setToppingUp] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const refresh = useCallback(async () => {
    try {
      setWallet(await getWallet(userId));
      setError("");
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, "Không thể tải thông tin ví."));
    } finally {
      setLoading(false);
    }
  }, [userId]);

  useEffect(() => {
    setLoading(true);
    setSuccess("");
    void refresh();
    const timer = window.setInterval(refresh, 3_000);
    return () => window.clearInterval(timer);
  }, [refresh]);

  async function topUp() {
    setToppingUp(true);
    setError("");
    setSuccess("");
    try {
      const updated = await topUpWallet(userId, DEMO_TOP_UP);
      setWallet(updated);
      setSuccess(`Đã nạp demo ${formatMoney(DEMO_TOP_UP)}. Transaction có idempotency key riêng.`);
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, "Không thể nạp tiền demo."));
    } finally {
      setToppingUp(false);
    }
  }

  return (
    <main className="mx-auto min-h-[calc(100vh-4rem)] max-w-6xl px-5 py-10 sm:px-8 sm:py-16">
      <div className="mb-9 flex flex-col justify-between gap-5 sm:flex-row sm:items-end">
        <div>
          <Link href="/" className="mb-5 inline-block text-sm font-bold text-slate-500 hover:text-orange-600">← Về trang đấu giá</Link>
          <p className="text-xs font-bold uppercase tracking-[0.2em] text-orange-600">Wallet service · gRPC core</p>
          <h1 className="mt-2 text-4xl font-black tracking-tight text-slate-950 sm:text-5xl">Ví OmniBid</h1>
          <p className="mt-3 max-w-xl text-slate-600">Theo dõi số dư khả dụng và phần tiền cọc đang được khóa trong các phiên đấu giá.</p>
        </div>
        <div>
          <label htmlFor="wallet-user" className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Ví demo</label>
          <select
            id="wallet-user"
            value={userId}
            onChange={(event) => setUserId(event.target.value)}
            className="h-12 rounded-xl border border-slate-300 bg-white px-4 text-sm font-semibold text-slate-800 outline-none focus:border-orange-500 focus:ring-4 focus:ring-orange-100"
          >
            {DEMO_USERS.map((user) => <option key={user.id} value={user.id}>{user.label} · {anonymizeUserId(user.id)}</option>)}
          </select>
        </div>
      </div>

      {error && <p className="mb-6 rounded-2xl border border-rose-200 bg-rose-50 p-4 text-sm font-medium text-rose-700">{error}</p>}
      {success && <p className="mb-6 rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-sm font-medium text-emerald-700">{success}</p>}

      {loading || !wallet ? (
        <div className="grid gap-6 lg:grid-cols-[1.3fr_0.7fr]">
          <Skeleton className="h-80" />
          <Skeleton className="h-80" />
        </div>
      ) : (
        <div className="grid gap-6 lg:grid-cols-[1.3fr_0.7fr]">
          <Card className="relative overflow-hidden !border-slate-900 !bg-slate-950 !text-white shadow-2xl">
            <div className="absolute -right-20 -top-24 size-72 rounded-full bg-orange-500/20 blur-3xl" />
            <div className="absolute -bottom-28 left-20 size-72 rounded-full bg-blue-500/10 blur-3xl" />
            <CardContent className="relative p-7 sm:p-10">
              <div className="flex items-start justify-between">
                <div>
                  <p className="text-xs font-bold uppercase tracking-[0.2em] text-slate-400">Available balance</p>
                  <p className="mt-3 text-4xl font-black tracking-tight text-white sm:text-6xl">{formatMoney(Number(wallet.availableBalance))}</p>
                </div>
                <span className="grid size-12 place-items-center rounded-2xl border border-white/10 bg-white/5 text-xl">◈</span>
              </div>

              <div className="mt-12 grid gap-4 border-t border-white/10 pt-7 sm:grid-cols-2">
                <div>
                  <p className="text-xs font-medium text-slate-500">Tổng tài sản</p>
                  <p className="mt-1 text-xl font-black text-slate-100">{formatMoney(Number(wallet.balance))}</p>
                </div>
                <div>
                  <p className="text-xs font-medium text-slate-500">Đang khóa cọc</p>
                  <p className="mt-1 text-xl font-black text-orange-400">{formatMoney(Number(wallet.frozenBalance))}</p>
                </div>
              </div>

              <div className="mt-8 flex items-center justify-between font-mono text-xs text-slate-500">
                <span>{anonymizeUserId(wallet.userId)}</span>
                <span>OMNIBID DEMO WALLET</span>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="flex h-full flex-col p-7">
              <span className="grid size-12 place-items-center rounded-2xl bg-orange-100 text-xl text-orange-700">＋</span>
              <h2 className="mt-6 text-2xl font-black text-slate-950">Nạp tiền demo</h2>
              <p className="mt-3 text-sm leading-6 text-slate-600">Tăng số dư test để tiếp tục thực hành freeze deposit. Mỗi thao tác được bảo vệ bằng `X-Idempotency-Key`.</p>
              <div className="my-6 rounded-2xl bg-slate-50 p-5">
                <p className="text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Số tiền</p>
                <p className="mt-2 text-2xl font-black text-slate-950">{formatMoney(DEMO_TOP_UP)}</p>
              </div>
              <Button onClick={topUp} disabled={toppingUp} className="mt-auto w-full">
                {toppingUp ? (
                  <><span className="size-4 animate-spin rounded-full border-2 border-white/30 border-t-white" /> Đang xử lý...</>
                ) : "Nạp tiền demo"}
              </Button>
            </CardContent>
          </Card>
        </div>
      )}

      <div className="mt-7 grid gap-4 sm:grid-cols-3">
        {[
          ["01", "REST → Wallet", "Dashboard đọc dữ liệu trực tiếp từ wallet-service."],
          ["02", "Row-level Lock", "Top-up và freeze serialize trên PostgreSQL wallet row."],
          ["03", "Durable Idempotency", "Unique transaction key chặn thao tác tài chính lặp."]
        ].map(([number, title, description]) => (
          <div key={number} className="rounded-2xl border border-slate-200 bg-white p-5">
            <span className="font-mono text-xs font-bold text-orange-600">{number}</span>
            <h3 className="mt-3 font-black text-slate-950">{title}</h3>
            <p className="mt-2 text-sm leading-6 text-slate-500">{description}</p>
          </div>
        ))}
      </div>
    </main>
  );
}
