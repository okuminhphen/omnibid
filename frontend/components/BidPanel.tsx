"use client";

import axios from "axios";
import { FormEvent, useState } from "react";
import { placeBid } from "@/services/api";

const DEMO_BIDDER_ID = "22222222-2222-2222-2222-222222222222";

export function BidPanel({ auctionId, currentPrice, onPlaced }: {
  auctionId: string;
  currentPrice: number;
  onPlaced: () => void;
}) {
  const [amount, setAmount] = useState(String(Number(currentPrice) + 10));
  const [message, setMessage] = useState("");
  const [submitting, setSubmitting] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setMessage("");
    try {
      const bid = await placeBid(auctionId, {
        bidderId: DEMO_BIDDER_ID,
        amount: Number(amount)
      });
      setMessage(`Bid accepted: ${bid.bidId}`);
      onPlaced();
    } catch (error) {
      const detail = axios.isAxiosError(error)
        ? error.response?.data?.message ?? error.message
        : "Unknown error";
      setMessage(`Bid rejected: ${detail}`);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={submit} className="rounded-3xl border border-slate-800 bg-slate-900/80 p-7">
      <label className="mb-2 block text-sm font-semibold text-slate-300" htmlFor="amount">
        Giá muốn đặt (USD)
      </label>
      <input
        id="amount"
        type="number"
        min={Number(currentPrice) + 0.01}
        step="0.01"
        value={amount}
        onChange={(event) => setAmount(event.target.value)}
        className="mb-4 w-full rounded-xl border border-slate-700 bg-slate-950 px-4 py-3 text-xl text-white outline-none focus:border-orange-400"
      />
      <button
        disabled={submitting}
        className="w-full rounded-xl bg-signal px-5 py-3 font-black text-white transition hover:bg-orange-500"
      >
        {submitting ? "Đang xử lý lock + gRPC..." : "Place bid"}
      </button>
      {message && <p className="mt-4 break-all text-sm text-slate-300">{message}</p>}
    </form>
  );
}
