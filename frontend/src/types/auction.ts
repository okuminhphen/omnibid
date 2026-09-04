export type AuctionStatus = "PENDING" | "ACTIVE" | "ENDED";

export interface Auction {
  id: string;
  title: string;
  status: AuctionStatus;
  startingPrice: number;
  currentPrice: number;
  stepPrice: number;
  depositAmount: number;
  leadingBidderAlias: string | null;
  startTime: string;
  endTime: string;
  version: number;
}

export interface BidHistory {
  bidId: string;
  auctionId: string;
  bidderAlias: string;
  amount: number;
  placedAt: string;
}

export interface WalletInfo {
  id: string;
  userId: string;
  balance: number;
  frozenBalance: number;
  availableBalance: number;
  updatedAt: string | null;
}

export type WalletTransactionType = "TOP_UP" | "FREEZE" | "REFUND" | "DEDUCT" | "WITHDRAW";

export interface WalletTransaction {
  id: string;
  auctionId: string | null;
  amount: number;
  type: WalletTransactionType;
  status: "SUCCESS" | "FAILED";
  createdAt: string;
}

export interface BidResponse {
  bidId: string;
  auctionId: string;
  amount: number;
  placedAt: string;
}

export interface ApiError {
  code?: string;
  message?: string;
  timestamp?: string;
}
