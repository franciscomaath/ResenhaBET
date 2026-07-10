-- Soft delete columns for bet_slip, bet_slip_item, and transaction
ALTER TABLE bet_slip ADD COLUMN deleted_at TIMESTAMP NULL;
ALTER TABLE bet_slip_item ADD COLUMN deleted_at TIMESTAMP NULL;
ALTER TABLE transaction ADD COLUMN deleted_at TIMESTAMP NULL;

-- Indexes for query performance
CREATE INDEX idx_bet_slip_deleted_at ON bet_slip(deleted_at);
CREATE INDEX idx_bet_slip_item_deleted_at ON bet_slip_item(deleted_at);
CREATE INDEX idx_transaction_deleted_at ON transaction(deleted_at);
