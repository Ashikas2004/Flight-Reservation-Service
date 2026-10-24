-- V5: Generate seats for all seeded flights that have no seats yet
-- Layout per flight:
--   First Class  : rows 1-2,  cols A-D  →  8 seats  (+$500 extra)
--   Business     : rows 3-8,  cols A-F  → 36 seats  (+$200 extra)
--   Economy      : rows 9-N,  cols A-F  → remaining seats ($0 extra)
--
-- A DO block iterates every flight that has 0 seats and inserts the full cabin.

DO $$
DECLARE
    rec         RECORD;
    r           INT;
    col         CHAR;
    first_cols  CHAR[] := ARRAY['A','B','C','D'];
    all_cols    CHAR[] := ARRAY['A','B','C','D','E','F'];
    fixed_seats INT    := 44;   -- 8 first + 36 business
    eco_rows    INT;
BEGIN
    FOR rec IN
        SELECT f.id, f.total_seats, f.flight_number
        FROM   flights f
        WHERE  NOT EXISTS (SELECT 1 FROM seats s WHERE s.flight_id = f.id)
    LOOP
        -- First Class: rows 1-2, cols A-D
        FOR r IN 1..2 LOOP
            FOREACH col IN ARRAY first_cols LOOP
                INSERT INTO seats (flight_id, seat_number, seat_class, status, extra_price, created_at)
                VALUES (rec.id, r::text || col, 'FIRST', 'AVAILABLE', 500.00, NOW());
            END LOOP;
        END LOOP;

        -- Business: rows 3-8, cols A-F
        FOR r IN 3..8 LOOP
            FOREACH col IN ARRAY all_cols LOOP
                INSERT INTO seats (flight_id, seat_number, seat_class, status, extra_price, created_at)
                VALUES (rec.id, r::text || col, 'BUSINESS', 'AVAILABLE', 200.00, NOW());
            END LOOP;
        END LOOP;

        -- Economy: rows 9 onwards
        eco_rows := CEIL(GREATEST(rec.total_seats - fixed_seats, 0)::numeric / 6);
        FOR r IN 9..(8 + eco_rows) LOOP
            FOREACH col IN ARRAY all_cols LOOP
                INSERT INTO seats (flight_id, seat_number, seat_class, status, extra_price, created_at)
                VALUES (rec.id, r::text || col, 'ECONOMY', 'AVAILABLE', 0.00, NOW());
            END LOOP;
        END LOOP;

        RAISE NOTICE 'Generated seats for flight %', rec.flight_number;
    END LOOP;
END;
$$;

-- Update available_seats to match actual generated seat count for all flights
UPDATE flights f
SET    available_seats = (
           SELECT COUNT(*) FROM seats s
           WHERE  s.flight_id = f.id AND s.status = 'AVAILABLE'
       ),
       total_seats = (
           SELECT COUNT(*) FROM seats s
           WHERE  s.flight_id = f.id
       );
