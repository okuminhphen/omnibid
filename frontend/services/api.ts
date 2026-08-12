import axios from "axios";
import type { Auction, BidResponse, PlaceBidPayload } from "@/types/auction";

export const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_AUCTION_API_URL ?? "http://localhost:8080",
  timeout: 5_000,
  headers: {
    "Content-Type": "application/json"
  }
});

export async function listAuctions(): Promise<Auction[]> {
  const response = await api.get<Auction[]>("/api/v1/auctions");
  return response.data;
}

export async function getAuction(id: string): Promise<Auction> {
  const response = await api.get<Auction>(`/api/v1/auctions/${id}`);
  return response.data;
}

export async function placeBid(id: string, payload: PlaceBidPayload): Promise<BidResponse> {
  // A new user action gets a new key. Retrying this exact HTTP call should reuse
  // the key; production clients normally persist it until a terminal response.
  const idempotencyKey = crypto.randomUUID();
  const response = await api.post<BidResponse>(
    `/api/v1/auctions/${id}/bid`,
    payload,
    { headers: { "X-Idempotency-Key": idempotencyKey } }
  );
  return response.data;
}
