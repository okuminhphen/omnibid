import { walletApi } from "@/services/api";
import type { WalletInfo } from "@/types/auction";

export async function getWallet(userId: string): Promise<WalletInfo> {
  const response = await walletApi.get<WalletInfo>(`/api/v1/wallets/${userId}`);
  return response.data;
}

export async function topUpWallet(userId: string, amount: number): Promise<WalletInfo> {
  const response = await walletApi.post<WalletInfo>(`/api/v1/wallets/${userId}/top-up`, {
    amount
  });
  return response.data;
}
