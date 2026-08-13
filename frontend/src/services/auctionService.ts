import { api } from "@/services/api";
import type { Auction, BidHistory, BidResponse } from "@/types/auction";

export async function listAuctions(): Promise<Auction[]> {
  const response = await api.get<Auction[]>("/api/v1/auctions");
  return response.data;
}

export async function getAuctionDetail(id: string): Promise<Auction> {
  const response = await api.get<Auction>(`/api/v1/auctions/${id}`);
  return response.data;
}

export async function placeBid(
  auctionId: string,
  userId: string,
  amount: number
): Promise<BidResponse> {
  const response = await api.post<BidResponse>(`/api/v1/auctions/${auctionId}/bid`, {
    userId,
    bidAmount: amount
  });
  return response.data;
}

export async function getBidHistory(auctionId: string): Promise<BidHistory[]> {
  const response = await api.get<BidResponse[]>(`/api/v1/auctions/${auctionId}/bids`);
  return response.data.map((bid) => ({
    bidId: bid.bidId,
    auctionId: bid.auctionId,
    userId: bid.bidderId,
    amount: bid.amount,
    placedAt: bid.placedAt
  }));
}

export const auctionService = {
  listAuctions,
  getAuctionDetail,
  placeBid,
  getBidHistory
};
