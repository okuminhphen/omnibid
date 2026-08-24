import type { HTMLAttributes } from "react";
import { cn } from "@/lib/utils";

export function Badge({ className, ...props }: HTMLAttributes<HTMLSpanElement>) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-sm border border-stone-300 bg-[#fffdf9] px-2.5 py-1 text-[11px] font-semibold uppercase tracking-[0.12em] text-stone-600",
        className
      )}
      {...props}
    />
  );
}
