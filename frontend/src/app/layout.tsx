import type { Metadata } from "next";
import type { ReactNode } from "react";
import { AppProviders } from "@/components/AppProviders";
import { SiteHeader } from "@/components/SiteHeader";
import "./globals.css";

export const metadata: Metadata = {
  title: "OmniBid — Nhà đấu giá trực tuyến",
  description: "Nền tảng đấu giá trực tuyến dành cho những vật phẩm tuyển chọn."
};

export default function RootLayout({ children }: Readonly<{ children: ReactNode }>) {
  return (
    <html lang="vi" data-scroll-behavior="smooth">
      <body>
        <AppProviders>
          <SiteHeader />
          {children}
        </AppProviders>
      </body>
    </html>
  );
}
