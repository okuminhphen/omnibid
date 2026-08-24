import type { InputHTMLAttributes } from "react";
import { cn } from "@/lib/utils";

export function Input({ className, ...props }: InputHTMLAttributes<HTMLInputElement>) {
  return (
    <input
      className={cn(
        "h-12 w-full rounded border border-stone-400 bg-[#fffdf9] px-4 text-base text-stone-950 outline-none transition placeholder:text-stone-400 focus:border-[#b43a2f] focus:ring-2 focus:ring-[#b43a2f]/10 disabled:bg-stone-100",
        className
      )}
      {...props}
    />
  );
}
