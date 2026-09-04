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
        <p className="text-[10px] font-semibold uppercase tracking-[0.2em] text-[#b43a2f]">Ví cá nhân</p>
        <h1 className="display-serif mt-4 text-5xl text-stone-950">Đăng nhập để mở ví</h1>
        <p className="mt-4 leading-7 text-stone-600">Số dư và lịch sử giao dịch chỉ hiển thị cho chủ tài khoản.</p>
        <Link href="/login" className="mt-7 inline-block rounded border border-stone-950 bg-stone-950 px-6 py-3 font-semibold text-white hover:bg-[#b43a2f]">Đăng nhập</Link>
      </main>
    );
  }

  return (
    <main className="mx-auto min-h-[calc(100vh-4.5rem)] max-w-[1200px] px-5 py-10 sm:px-8 sm:py-16">
      <div className="mb-10 border-b border-stone-300 pb-7">
        <Link href="/" className="mb-7 inline-block text-sm text-stone-500 hover:text-[#b43a2f]">← Về trang đấu giá</Link>
        <p className="text-[10px] font-semibold uppercase tracking-[0.2em] text-[#b43a2f]">Tài khoản cá nhân</p>
        <h1 className="display-serif mt-3 text-5xl tracking-[-0.035em] text-stone-950 sm:text-6xl">Ví của {user?.displayName ?? "bạn"}</h1>
        <p className="mt-4 max-w-xl text-stone-600">Theo dõi số dư khả dụng, tiền cọc đang giữ và các giao dịch gần đây.</p>
      </div>

      {error && <p className="mb-6 border border-red-300 bg-red-50 p-4 text-sm text-red-800">{error}</p>}
      {success && <p className="mb-6 border border-emerald-300 bg-emerald-50 p-4 text-sm text-emerald-800">{success}</p>}

      {loading || !wallet ? (
        <div className="grid gap-6 lg:grid-cols-[1.3fr_0.7fr]"><Skeleton className="h-80" /><Skeleton className="h-80" /></div>
      ) : (
        <div className="grid gap-6 lg:grid-cols-[1.3fr_0.7fr]">
          <Card className="!border-stone-400 !bg-[#262521] !text-white">
            <CardContent className="p-7 sm:p-10">
              <p className="text-[10px] font-semibold uppercase tracking-[0.2em] text-stone-400">Số dư khả dụng</p>
              <p className="display-serif mt-4 text-4xl tracking-[-0.03em] text-white sm:text-6xl">{formatMoney(Number(wallet.availableBalance))}</p>
              <div className="mt-12 grid border-t border-white/20 pt-7 sm:grid-cols-2">
                <div><p className="text-xs text-stone-500">Tổng số dư</p><p className="mt-2 text-lg font-semibold">{formatMoney(Number(wallet.balance))}</p></div>
                <div className="mt-5 sm:mt-0 sm:border-l sm:border-white/20 sm:pl-7"><p className="text-xs text-stone-500">Đang giữ cọc</p><p className="mt-2 text-lg font-semibold text-[#e48277]">{formatMoney(Number(wallet.frozenBalance))}</p></div>
              </div>
              <div className="mt-10 flex items-center justify-between font-mono text-[10px] uppercase tracking-[0.12em] text-stone-500"><span>{anonymizeUserId(wallet.userId)}</span><span>OmniBid account</span></div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="flex h-full flex-col p-7">
              <h2 className="display-serif text-3xl text-stone-950">Nạp hoặc rút tiền</h2>
              <p className="mt-2 text-sm leading-6 text-stone-600">Đây là số dư thử nghiệm, không kết nối với tài khoản ngân hàng thật.</p>
              <form className="mt-6 space-y-4">
                <div className="relative">
                  <Input type="number" min="1" max="10000000" value={amount} onChange={(event) => setAmount(event.target.value)} className="h-14 pr-16 text-lg font-semibold" />
                  <span className="absolute right-4 top-1/2 -translate-y-1/2 text-xs font-semibold text-stone-400">VND</span>
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

      <Card className="mt-8">
        <CardContent className="p-0">
          <div className="border-b border-stone-300 px-6 py-5">
            <h2 className="display-serif text-2xl text-stone-950">Lịch sử giao dịch</h2>
            <p className="mt-1 text-xs text-stone-500">Các giao dịch gần nhất của tài khoản</p>
          </div>
          <div className="divide-y divide-stone-300">
            {transactions.map((transaction) => {
              const incoming = transaction.type === "TOP_UP" || transaction.type === "REFUND";
              return (
                <div key={transaction.id} className="flex items-center justify-between gap-4 px-6 py-4">
                  <div>
                    <p className="text-sm font-semibold text-stone-800">{TRANSACTION_LABELS[transaction.type]}</p>
                    <p className="mt-1 text-xs text-stone-400">{new Date(transaction.createdAt).toLocaleString("vi-VN")} · {transaction.id.slice(0, 8)}</p>
                  </div>
                  <p className={`font-semibold tabular-nums ${incoming ? "text-emerald-700" : "text-stone-900"}`}>{incoming ? "+" : "−"}{formatMoney(Number(transaction.amount))}</p>
                </div>
              );
            })}
            {!loading && transactions.length === 0 && <p className="px-6 py-12 text-center text-sm text-stone-500">Chưa có giao dịch.</p>}
          </div>
        </CardContent>
      </Card>
    </main>
  );
}
