export type AuctionStatus = "DRAFT" | "ACTIVE" | "ENDED" | "CANCELLED";

export interface Auction {
  id: string;
  title: string;
  status: AuctionStatus;
  currentPrice: number;
  highestBidderId: string | null;
  endsAt: string;
  version: number;
}

export interface PlaceBidPayload {
  userId: string;
  bidAmount: number;
}

export interface BidResponse {
  bidId: string;
  auctionId: string;
  bidderId: string;
  amount: number;
  walletTransactionId: string;
  placedAt: string;
}
