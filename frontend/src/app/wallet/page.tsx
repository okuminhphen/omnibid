"use client";

import Link from "next/link";
import { useCallback, useEffect, useState, type FormEvent } from "react";
import { useAuth } from "@/components/AuthProvider";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { getApiErrorMessage } from "@/lib/errors";
import { anonymizeUserId, formatMoney } from "@/lib/utils";
import {
  getMyWallet,
  getMyWalletTransactions,
  topUpMyWallet,
  withdrawMyWallet
} from "@/services/walletService";
import type { WalletInfo, WalletTransaction } from "@/types/auction";

const DEFAULT_AMOUNT = 500_000;

const TRANSACTION_LABELS: Record<WalletTransaction["type"], string> = {
  TOP_UP: "Nạp demo",
  WITHDRAW: "Rút demo",
  FREEZE: "Khóa cọc",
  REFUND: "Hoàn cọc",
  DEDUCT: "Thanh toán"
};

export default function WalletPage() {
  const { user, hydrated } = useAuth();
  const [wallet, setWallet] = useState<WalletInfo | null>(null);
  const [transactions, setTransactions] = useState<WalletTransaction[]>([]);
  const [amount, setAmount] = useState(String(DEFAULT_AMOUNT));
  const [loading, setLoading] = useState(true);
  const [operation, setOperation] = useState<"top-up" | "withdraw" | null>(null);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const refresh = useCallback(async () => {
    if (!user) return;
    try {
      const [nextWallet, nextTransactions] = await Promise.all([
        getMyWallet(),
        getMyWalletTransactions()
      ]);
      setWallet(nextWallet);
      setTransactions(nextTransactions);
      setError("");
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, "Không thể tải thông tin ví."));
    } finally {
      setLoading(false);
    }
  }, [user]);

  useEffect(() => {
    if (!hydrated || !user) {
      setLoading(false);
      return;
    }
    setLoading(true);
    void refresh();
    const timer = window.setInterval(refresh, 5_000);
    return () => window.clearInterval(timer);
  }, [hydrated, refresh, user]);

  async function mutateWallet(event: FormEvent, type: "top-up" | "withdraw") {
    event.preventDefault();
    const numericAmount = Number(amount);
    if (!Number.isFinite(numericAmount) || numericAmount <= 0) {
      setError("Số tiền phải lớn hơn 0.");
      return;
    }

    setOperation(type);
    setError("");
    setSuccess("");
    try {
      const updated = type === "top-up"
        ? await topUpMyWallet(numericAmount)
        : await withdrawMyWallet(numericAmount);
      setWallet(updated);
      setSuccess(`${type === "top-up" ? "Nạp" : "Rút"} demo ${formatMoney(numericAmount)} thành công.`);
      await refresh();
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, `Không thể ${type === "top-up" ? "nạp" : "rút"} tiền demo.`));
    } finally {
      setOperation(null);
    }
  }

  if (hydrated && !user) {
    return (
      <main className="mx-auto max-w-xl px-5 py-20 text-center">
        <p className="text-xs font-bold uppercase tracking-[0.2em] text-orange-600">Protected wallet</p>
        <h1 className="mt-3 text-4xl font-black text-slate-950">Đăng nhập để mở ví</h1>
        <p className="mt-4 leading-7 text-slate-600">Wallet service chỉ tin `sub` đã ký trong JWT, không nhận userId từ trình duyệt.</p>
        <Link href="/login" className="mt-7 inline-block rounded-xl bg-slate-950 px-6 py-3 font-bold text-white hover:bg-orange-500">Đăng nhập</Link>
      </main>
    );
  }

  return (
    <main className="mx-auto min-h-[calc(100vh-4rem)] max-w-6xl px-5 py-10 sm:px-8 sm:py-16">
      <div className="mb-9">
        <Link href="/" className="mb-5 inline-block text-sm font-bold text-slate-500 hover:text-orange-600">← Về trang đấu giá</Link>
        <p className="text-xs font-bold uppercase tracking-[0.2em] text-orange-600">JWT protected · personal wallet</p>
        <h1 className="mt-2 text-4xl font-black tracking-tight text-slate-950 sm:text-5xl">Ví của {user?.displayName ?? "bạn"}</h1>
        <p className="mt-3 max-w-xl text-slate-600">Nạp/rút tiền mô phỏng, theo dõi số dư khả dụng và cọc đang khóa.</p>
      </div>

      {error && <p className="mb-6 rounded-2xl border border-rose-200 bg-rose-50 p-4 text-sm font-medium text-rose-700">{error}</p>}
      {success && <p className="mb-6 rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-sm font-medium text-emerald-700">{success}</p>}

      {loading || !wallet ? (
        <div className="grid gap-6 lg:grid-cols-[1.3fr_0.7fr]"><Skeleton className="h-80" /><Skeleton className="h-80" /></div>
      ) : (
        <div className="grid gap-6 lg:grid-cols-[1.3fr_0.7fr]">
          <Card className="relative overflow-hidden !border-slate-900 !bg-slate-950 !text-white shadow-2xl">
            <div className="absolute -right-20 -top-24 size-72 rounded-full bg-orange-500/20 blur-3xl" />
            <CardContent className="relative p-7 sm:p-10">
              <p className="text-xs font-bold uppercase tracking-[0.2em] text-slate-400">Available balance</p>
              <p className="mt-3 text-4xl font-black tracking-tight text-white sm:text-6xl">{formatMoney(Number(wallet.availableBalance))}</p>
              <div className="mt-12 grid gap-4 border-t border-white/10 pt-7 sm:grid-cols-2">
                <div><p className="text-xs text-slate-500">Tổng tài sản</p><p className="mt-1 text-xl font-black">{formatMoney(Number(wallet.balance))}</p></div>
                <div><p className="text-xs text-slate-500">Đang khóa cọc</p><p className="mt-1 text-xl font-black text-orange-400">{formatMoney(Number(wallet.frozenBalance))}</p></div>
              </div>
              <div className="mt-8 flex items-center justify-between font-mono text-xs text-slate-500"><span>{anonymizeUserId(wallet.userId)}</span><span>OMNIBID WALLET</span></div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="flex h-full flex-col p-7">
              <h2 className="text-2xl font-black text-slate-950">Giao dịch demo</h2>
              <p className="mt-2 text-sm leading-6 text-slate-600">Không kết nối ngân hàng thật. Mỗi lệnh có idempotency key riêng và được ghi transaction bất biến.</p>
              <form className="mt-6 space-y-4">
                <div className="relative">
                  <Input type="number" min="1" max="10000000" value={amount} onChange={(event) => setAmount(event.target.value)} className="h-14 pr-16 text-lg font-black" />
                  <span className="absolute right-4 top-1/2 -translate-y-1/2 text-xs font-bold text-slate-400">VND</span>
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <Button disabled={Boolean(operation)} onClick={(event) => void mutateWallet(event, "top-up")}>{operation === "top-up" ? "Đang nạp..." : "Nạp demo"}</Button>
                  <Button variant="outline" disabled={Boolean(operation)} onClick={(event) => void mutateWallet(event, "withdraw")}>{operation === "withdraw" ? "Đang rút..." : "Rút demo"}</Button>
                </div>
              </form>
            </CardContent>
          </Card>
        </div>
      )}

      <Card className="mt-7">
        <CardContent className="p-0">
          <div className="border-b border-slate-100 px-6 py-5">
            <h2 className="font-black text-slate-950">Lịch sử giao dịch</h2>
            <p className="mt-1 text-xs text-slate-500">100 giao dịch gần nhất của chính tài khoản đang đăng nhập</p>
          </div>
          <div className="divide-y divide-slate-100">
            {transactions.map((transaction) => {
              const incoming = transaction.type === "TOP_UP" || transaction.type === "REFUND";
              return (
                <div key={transaction.id} className="flex items-center justify-between gap-4 px-6 py-4">
                  <div>
                    <p className="text-sm font-bold text-slate-800">{TRANSACTION_LABELS[transaction.type]}</p>
                    <p className="mt-1 text-xs text-slate-400">{new Date(transaction.createdAt).toLocaleString("vi-VN")} · {transaction.id.slice(0, 8)}</p>
                  </div>
                  <p className={`font-black ${incoming ? "text-emerald-600" : "text-slate-900"}`}>{incoming ? "+" : "−"}{formatMoney(Number(transaction.amount))}</p>
                </div>
              );
            })}
            {!loading && transactions.length === 0 && <p className="px-6 py-12 text-center text-sm text-slate-500">Chưa có giao dịch.</p>}
          </div>
        </CardContent>
      </Card>
    </main>
  );
}
