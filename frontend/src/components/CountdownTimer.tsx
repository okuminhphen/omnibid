"use client";

import { useEffect, useState } from "react";

function getRemaining(target: string): number {
  return Math.max(0, new Date(target).getTime() - Date.now());
}

function Segment({ value, label }: { value: string; label: string }) {
  return (
    <div className="min-w-14 border-l border-stone-300 px-3 first:border-l-0 first:pl-0 sm:px-4">
      <strong className="display-serif block text-3xl font-normal tabular-nums text-stone-950">{value}</strong>
      <span className="text-[9px] font-semibold uppercase tracking-[0.16em] text-stone-500">{label}</span>
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
    return <div className="h-[62px] animate-pulse bg-stone-200" />;
  }
  if (remaining === 0) {
    return <p className="border border-red-300 bg-red-50 px-4 py-5 text-center font-semibold text-red-800">Phiên đã kết thúc</p>;
  }

  const totalSeconds = Math.floor(remaining / 1_000);
  const days = Math.floor(totalSeconds / 86_400);
  const hours = Math.floor((totalSeconds % 86_400) / 3_600);
  const minutes = Math.floor((totalSeconds % 3_600) / 60);
  const seconds = totalSeconds % 60;
  const pad = (value: number) => String(value).padStart(2, "0");

  return (
    <div className="grid grid-cols-4" aria-label="Thời gian còn lại">
      <Segment value={pad(days)} label="Ngày" />
      <Segment value={pad(hours)} label="Giờ" />
      <Segment value={pad(minutes)} label="Phút" />
      <Segment value={pad(seconds)} label="Giây" />
    </div>
  );
}
