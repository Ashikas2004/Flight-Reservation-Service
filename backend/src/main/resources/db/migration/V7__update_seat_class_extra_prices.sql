-- Update the default surcharges for existing first-class and business seats.
UPDATE seats
SET extra_price = 6000.00
WHERE seat_class = 'FIRST'
  AND extra_price = 500.00;

UPDATE seats
SET extra_price = 3000.00
WHERE seat_class = 'BUSINESS'
  AND extra_price = 200.00;
