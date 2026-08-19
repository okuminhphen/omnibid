import type { Metadata } from "next";
import type { ReactNode } from "react";
import { AppProviders } from "@/components/AppProviders";
import { SiteHeader } from "@/components/SiteHeader";
import "./globals.css";

export const metadata: Metadata = {
  title: "OmniBid — Real-time Auction",
  description: "Sàn đấu giá real-time xây dựng trên Redis Lock, gRPC, Kafka và RabbitMQ"
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
