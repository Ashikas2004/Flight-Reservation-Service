-- V6: Add lock tracking columns to seats table for concurrency control

ALTER TABLE seats
    ADD COLUMN IF NOT EXISTS locked_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS locked_by VARCHAR(100);

-- Index to speed up the expiry job query
CREATE INDEX IF NOT EXISTS idx_seats_locked_at
    ON seats (locked_at)
    WHERE status = 'LOCKED';

COMMENT ON COLUMN seats.locked_at IS
    'Timestamp when this seat was locked. Used to auto-expire stale locks after 10 minutes.';
COMMENT ON COLUMN seats.locked_by IS
    'Email of the user who locked this seat.';
