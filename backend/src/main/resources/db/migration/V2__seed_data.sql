-- ============================================================
-- V2: Seed data — airlines, airports, admin user, sample flights
-- ============================================================

-- Admin user (password: Admin@1234 — BCrypt hash)
INSERT INTO users (email, password, first_name, last_name, role, enabled, created_by)
VALUES ('admin@flightreservation.com',
        '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
        'System', 'Admin', 'ADMIN', TRUE, 'system')
ON CONFLICT (email) DO NOTHING;

-- Airlines
INSERT INTO airlines (code, name, country, active, created_by) VALUES
('AA',  'American Airlines',    'United States', TRUE, 'system'),
('UA',  'United Airlines',      'United States', TRUE, 'system'),
('DL',  'Delta Air Lines',      'United States', TRUE, 'system'),
('BA',  'British Airways',      'United Kingdom',TRUE, 'system'),
('EK',  'Emirates',             'UAE',           TRUE, 'system'),
('SQ',  'Singapore Airlines',   'Singapore',     TRUE, 'system'),
('QF',  'Qantas',               'Australia',     TRUE, 'system'),
('LH',  'Lufthansa',            'Germany',       TRUE, 'system')
ON CONFLICT (code) DO NOTHING;

-- Airports
INSERT INTO airports (iata_code, name, city, country, timezone, active, created_by) VALUES
('JFK', 'John F. Kennedy International',  'New York',      'United States', 'America/New_York',   TRUE, 'system'),
('LAX', 'Los Angeles International',       'Los Angeles',   'United States', 'America/Los_Angeles',TRUE, 'system'),
('ORD', 'O''Hare International',           'Chicago',       'United States', 'America/Chicago',    TRUE, 'system'),
('MIA', 'Miami International',             'Miami',         'United States', 'America/New_York',   TRUE, 'system'),
('SFO', 'San Francisco International',     'San Francisco', 'United States', 'America/Los_Angeles',TRUE, 'system'),
('LHR', 'Heathrow Airport',               'London',        'United Kingdom','Europe/London',       TRUE, 'system'),
('DXB', 'Dubai International',             'Dubai',         'UAE',           'Asia/Dubai',          TRUE, 'system'),
('SIN', 'Changi Airport',                  'Singapore',     'Singapore',     'Asia/Singapore',      TRUE, 'system'),
('SYD', 'Kingsford Smith International',   'Sydney',        'Australia',     'Australia/Sydney',    TRUE, 'system'),
('FRA', 'Frankfurt Airport',               'Frankfurt',     'Germany',       'Europe/Berlin',       TRUE, 'system'),
('DEL', 'Indira Gandhi International',     'New Delhi',     'India',         'Asia/Kolkata',        TRUE, 'system'),
('NRT', 'Narita International',            'Tokyo',         'Japan',         'Asia/Tokyo',          TRUE, 'system')
ON CONFLICT (iata_code) DO NOTHING;

-- Sample flights (using subqueries to resolve IDs)
INSERT INTO flights (flight_number, airline_id, origin_airport_id, destination_airport_id,
                     departure_time, arrival_time, base_price, total_seats, available_seats, status, created_by)
SELECT 'AA101',
       (SELECT id FROM airlines WHERE code = 'AA'),
       (SELECT id FROM airports WHERE iata_code = 'JFK'),
       (SELECT id FROM airports WHERE iata_code = 'LAX'),
       NOW() + INTERVAL '2 days',
       NOW() + INTERVAL '2 days' + INTERVAL '6 hours',
       299.00, 180, 180, 'SCHEDULED', 'system'
WHERE NOT EXISTS (SELECT 1 FROM flights WHERE flight_number = 'AA101');

INSERT INTO flights (flight_number, airline_id, origin_airport_id, destination_airport_id,
                     departure_time, arrival_time, base_price, total_seats, available_seats, status, created_by)
SELECT 'UA202',
       (SELECT id FROM airlines WHERE code = 'UA'),
       (SELECT id FROM airports WHERE iata_code = 'LAX'),
       (SELECT id FROM airports WHERE iata_code = 'ORD'),
       NOW() + INTERVAL '3 days',
       NOW() + INTERVAL '3 days' + INTERVAL '4 hours 30 minutes',
       199.00, 160, 160, 'SCHEDULED', 'system'
WHERE NOT EXISTS (SELECT 1 FROM flights WHERE flight_number = 'UA202');

INSERT INTO flights (flight_number, airline_id, origin_airport_id, destination_airport_id,
                     departure_time, arrival_time, base_price, total_seats, available_seats, status, created_by)
SELECT 'EK501',
       (SELECT id FROM airlines WHERE code = 'EK'),
       (SELECT id FROM airports WHERE iata_code = 'DXB'),
       (SELECT id FROM airports WHERE iata_code = 'LHR'),
       NOW() + INTERVAL '4 days',
       NOW() + INTERVAL '4 days' + INTERVAL '7 hours 15 minutes',
       649.00, 200, 200, 'SCHEDULED', 'system'
WHERE NOT EXISTS (SELECT 1 FROM flights WHERE flight_number = 'EK501');

INSERT INTO flights (flight_number, airline_id, origin_airport_id, destination_airport_id,
                     departure_time, arrival_time, base_price, total_seats, available_seats, status, created_by)
SELECT 'SQ321',
       (SELECT id FROM airlines WHERE code = 'SQ'),
       (SELECT id FROM airports WHERE iata_code = 'SIN'),
       (SELECT id FROM airports WHERE iata_code = 'SYD'),
       NOW() + INTERVAL '5 days',
       NOW() + INTERVAL '5 days' + INTERVAL '8 hours',
       549.00, 210, 210, 'SCHEDULED', 'system'
WHERE NOT EXISTS (SELECT 1 FROM flights WHERE flight_number = 'SQ321');

INSERT INTO flights (flight_number, airline_id, origin_airport_id, destination_airport_id,
                     departure_time, arrival_time, base_price, total_seats, available_seats, status, created_by)
SELECT 'BA178',
       (SELECT id FROM airlines WHERE code = 'BA'),
       (SELECT id FROM airports WHERE iata_code = 'LHR'),
       (SELECT id FROM airports WHERE iata_code = 'JFK'),
       NOW() + INTERVAL '6 days',
       NOW() + INTERVAL '6 days' + INTERVAL '8 hours 30 minutes',
       799.00, 220, 220, 'SCHEDULED', 'system'
WHERE NOT EXISTS (SELECT 1 FROM flights WHERE flight_number = 'BA178');
