import { walletApi } from "@/services/api";
import type { WalletInfo, WalletTransaction } from "@/types/auction";

export async function getMyWallet(): Promise<WalletInfo> {
  const response = await walletApi.get<WalletInfo>("/api/v1/me/wallet");
  return response.data;
}

export async function topUpMyWallet(amount: number): Promise<WalletInfo> {
  const response = await walletApi.post<WalletInfo>("/api/v1/me/wallet/top-ups", {
    amount
  });
  return response.data;
}

export async function withdrawMyWallet(amount: number): Promise<WalletInfo> {
  const response = await walletApi.post<WalletInfo>("/api/v1/me/wallet/withdrawals", {
    amount
  });
  return response.data;
}

export async function getMyWalletTransactions(): Promise<WalletTransaction[]> {
  const response = await walletApi.get<WalletTransaction[]>("/api/v1/me/wallet/transactions");
  return response.data;
}
