ALTER TABLE flights
    DROP CONSTRAINT IF EXISTS flights_flight_number_key;

ALTER TABLE flights
    ADD CONSTRAINT uk_flight_number_departure UNIQUE (flight_number, departure_time);

ALTER TABLE flights
    ADD COLUMN schedule_routine VARCHAR(30) NOT NULL DEFAULT 'SPECIAL_ADDITIONAL',
    ADD COLUMN schedule_series_id VARCHAR(36),
    ADD COLUMN recurrence_end_date DATE;
