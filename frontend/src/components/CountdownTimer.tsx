"use client";

import { useEffect, useState } from "react";

function getRemaining(target: string): number {
  return Math.max(0, new Date(target).getTime() - Date.now());
}

function Segment({ value, label }: { value: string; label: string }) {
  return (
    <div className="min-w-16 rounded-2xl border border-white/10 bg-white/5 px-3 py-3 text-center">
      <strong className="block text-2xl font-black tabular-nums text-white">{value}</strong>
      <span className="text-[10px] font-bold uppercase tracking-[0.18em] text-slate-400">{label}</span>
    </div>
  );
}

export function CountdownTimer({ endTime }: { endTime: string }) {
  const [remaining, setRemaining] = useState<number | null>(null);

  useEffect(() => {
    const update = () => setRemaining(getRemaining(endTime));
    update();
    const timer = window.setInterval(update, 1_000);
    return () => window.clearInterval(timer);
  }, [endTime]);

  if (remaining === null) {
    return <div className="h-[74px] animate-pulse rounded-2xl bg-white/5" />;
  }
  if (remaining === 0) {
    return <p className="rounded-2xl bg-rose-500/15 px-4 py-5 text-center font-bold text-rose-300">Phiên đã kết thúc</p>;
  }

  const totalSeconds = Math.floor(remaining / 1_000);
  const days = Math.floor(totalSeconds / 86_400);
  const hours = Math.floor((totalSeconds % 86_400) / 3_600);
  const minutes = Math.floor((totalSeconds % 3_600) / 60);
  const seconds = totalSeconds % 60;
  const pad = (value: number) => String(value).padStart(2, "0");

  return (
    <div className="grid grid-cols-4 gap-2" aria-label="Thời gian còn lại">
      <Segment value={pad(days)} label="Ngày" />
      <Segment value={pad(hours)} label="Giờ" />
      <Segment value={pad(minutes)} label="Phút" />
      <Segment value={pad(seconds)} label="Giây" />
    </div>
  );
}
