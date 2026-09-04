CREATE INDEX IF NOT EXISTS idx_bids_auction_bidder_placed_at
    ON bids (auction_id, bidder_id, placed_at ASC);
