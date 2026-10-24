ALTER TABLE payments
    ALTER COLUMN currency SET DEFAULT 'INR';

ALTER TABLE refunds
    ALTER COLUMN currency SET DEFAULT 'INR';

UPDATE payments
SET currency = 'INR'
WHERE currency = 'USD';

UPDATE refunds
SET currency = 'INR'
WHERE currency = 'USD';
