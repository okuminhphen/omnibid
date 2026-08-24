import type { ButtonHTMLAttributes } from "react";
import { cn } from "@/lib/utils";

type ButtonVariant = "primary" | "secondary" | "outline" | "ghost";

const variants: Record<ButtonVariant, string> = {
  primary: "border border-[#171714] bg-[#171714] text-white hover:bg-[#b43a2f] hover:border-[#b43a2f]",
  secondary: "border border-[#b43a2f] bg-[#b43a2f] text-white hover:bg-[#8f2c24]",
  outline: "border border-stone-400 bg-transparent text-stone-900 hover:border-stone-950 hover:bg-stone-100",
  ghost: "text-stone-600 hover:bg-stone-200/70 hover:text-stone-950"
};

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
}

export function Button({ className, variant = "primary", ...props }: ButtonProps) {
  return (
    <button
      className={cn(
        "inline-flex min-h-11 items-center justify-center gap-2 rounded px-5 text-sm font-semibold transition duration-150 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#b43a2f] focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-45",
        variants[variant],
        className
      )}
      {...props}
    />
  );
}
