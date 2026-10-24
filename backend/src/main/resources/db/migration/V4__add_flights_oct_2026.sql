-- V4: Add flights from Oct 7–12 2026 across all seeded airlines and airports

-- Helper: all inserts use absolute timestamps (UTC)

-- Oct 7
INSERT INTO flights (flight_number, airline_id, origin_airport_id, destination_airport_id,
                     departure_time, arrival_time, base_price, total_seats, available_seats, status, created_by)
SELECT 'AA201', (SELECT id FROM airlines WHERE code='AA'),
       (SELECT id FROM airports WHERE iata_code='JFK'), (SELECT id FROM airports WHERE iata_code='LAX'),
       '2026-10-07 06:00:00', '2026-10-07 12:00:00', 319.00, 180, 180, 'SCHEDULED', 'system'
WHERE NOT EXISTS (SELECT 1 FROM flights WHERE flight_number='AA201');

INSERT INTO flights (flight_number, airline_id, origin_airport_id, destination_airport_id,
                     departure_time, arrival_time, base_price, total_seats, available_seats, status, created_by)
SELECT 'EK601', (SELECT id FROM airlines WHERE code='EK'),
       (SELECT id FROM airports WHERE iata_code='DXB'), (SELECT id FROM airports WHERE iata_code='LHR'),
       '2026-10-07 08:30:00', '2026-10-07 13:45:00', 689.00, 200, 200, 'SCHEDULED', 'system'
WHERE NOT EXISTS (SELECT 1 FROM flights WHERE flight_number='EK601');

INSERT INTO flights (flight_number, airline_id, origin_airport_id, destination_airport_id,
                     departure_time, arrival_time, base_price, total_seats, available_seats, status, created_by)
SELECT 'LH400', (SELECT id FROM airlines WHERE code='LH'),
       (SELECT id FROM airports WHERE iata_code='FRA'), (SELECT id FROM airports WHERE iata_code='JFK'),
       '2026-10-07 10:00:00', '2026-10-07 20:30:00', 749.00, 210, 210, 'SCHEDULED', 'system'
WHERE NOT EXISTS (SELECT 1 FROM flights WHERE flight_number='LH400');

-- Oct 8
INSERT INTO flights (flight_number, airline_id, origin_airport_id, destination_airport_id,
                     departure_time, arrival_time, base_price, total_seats, available_seats, status, created_by)
SELECT 'UA301', (SELECT id FROM airlines WHERE code='UA'),
       (SELECT id FROM airports WHERE iata_code='ORD'), (SELECT id FROM airports WHERE iata_code='SFO'),
       '2026-10-08 07:00:00', '2026-10-08 10:30:00', 229.00, 160, 160, 'SCHEDULED', 'system'
WHERE NOT EXISTS (SELECT 1 FROM flights WHERE flight_number='UA301');

INSERT INTO flights (flight_number, airline_id, origin_airport_id, destination_airport_id,
                     departure_time, arrival_time, base_price, total_seats, available_seats, status, created_by)
SELECT 'SQ422', (SELECT id FROM airlines WHERE code='SQ'),
       (SELECT id FROM airports WHERE iata_code='SIN'), (SELECT id FROM airports WHERE iata_code='NRT'),
       '2026-10-08 09:15:00', '2026-10-08 17:00:00', 499.00, 210, 210, 'SCHEDULED', 'system'
WHERE NOT EXISTS (SELECT 1 FROM flights WHERE flight_number='SQ422');

INSERT INTO flights (flight_number, airline_id, origin_airport_id, destination_airport_id,
                     departure_time, arrival_time, base_price, total_seats, available_seats, status, created_by)
SELECT 'BA299', (SELECT id FROM airlines WHERE code='BA'),
       (SELECT id FROM airports WHERE iata_code='LHR'), (SELECT id FROM airports WHERE iata_code='DEL'),
       '2026-10-08 14:00:00', '2026-10-09 01:30:00', 829.00, 220, 220, 'SCHEDULED', 'system'
WHERE NOT EXISTS (SELECT 1 FROM flights WHERE flight_number='BA299');

-- Oct 9
INSERT INTO flights (flight_number, airline_id, origin_airport_id, destination_airport_id,
                     departure_time, arrival_time, base_price, total_seats, available_seats, status, created_by)
SELECT 'DL510', (SELECT id FROM airlines WHERE code='DL'),
       (SELECT id FROM airports WHERE iata_code='MIA'), (SELECT id FROM airports WHERE iata_code='JFK'),
       '2026-10-09 08:00:00', '2026-10-09 11:15:00', 189.00, 170, 170, 'SCHEDULED', 'system'
WHERE NOT EXISTS (SELECT 1 FROM flights WHERE flight_number='DL510');

INSERT INTO flights (flight_number, airline_id, origin_airport_id, destination_airport_id,
                     departure_time, arrival_time, base_price, total_seats, available_seats, status, created_by)
SELECT 'EK702', (SELECT id FROM airlines WHERE code='EK'),
       (SELECT id FROM airports WHERE iata_code='DXB'), (SELECT id FROM airports WHERE iata_code='SIN'),
       '2026-10-09 02:30:00', '2026-10-09 14:00:00', 579.00, 200, 200, 'SCHEDULED', 'system'
WHERE NOT EXISTS (SELECT 1 FROM flights WHERE flight_number='EK702');

INSERT INTO flights (flight_number, airline_id, origin_airport_id, destination_airport_id,
                     departure_time, arrival_time, base_price, total_seats, available_seats, status, created_by)
SELECT 'QF010', (SELECT id FROM airlines WHERE code='QF'),
       (SELECT id FROM airports WHERE iata_code='SYD'), (SELECT id FROM airports WHERE iata_code='LHR'),
       '2026-10-09 16:00:00', '2026-10-10 05:00:00', 1099.00, 190, 190, 'SCHEDULED', 'system'
WHERE NOT EXISTS (SELECT 1 FROM flights WHERE flight_number='QF010');

-- Oct 10
INSERT INTO flights (flight_number, airline_id, origin_airport_id, destination_airport_id,
                     departure_time, arrival_time, base_price, total_seats, available_seats, status, created_by)
SELECT 'AA305', (SELECT id FROM airlines WHERE code='AA'),
       (SELECT id FROM airports WHERE iata_code='LAX'), (SELECT id FROM airports WHERE iata_code='ORD'),
       '2026-10-10 06:30:00', '2026-10-10 12:00:00', 249.00, 180, 180, 'SCHEDULED', 'system'
WHERE NOT EXISTS (SELECT 1 FROM flights WHERE flight_number='AA305');

INSERT INTO flights (flight_number, airline_id, origin_airport_id, destination_airport_id,
                     departure_time, arrival_time, base_price, total_seats, available_seats, status, created_by)
SELECT 'LH510', (SELECT id FROM airlines WHERE code='LH'),
       (SELECT id FROM airports WHERE iata_code='FRA'), (SELECT id FROM airports WHERE iata_code='SIN'),
       '2026-10-10 11:00:00', '2026-10-11 05:30:00', 879.00, 210, 210, 'SCHEDULED', 'system'
WHERE NOT EXISTS (SELECT 1 FROM flights WHERE flight_number='LH510');

INSERT INTO flights (flight_number, airline_id, origin_airport_id, destination_airport_id,
                     departure_time, arrival_time, base_price, total_seats, available_seats, status, created_by)
SELECT 'SQ550', (SELECT id FROM airlines WHERE code='SQ'),
       (SELECT id FROM airports WHERE iata_code='SIN'), (SELECT id FROM airports WHERE iata_code='DEL'),
       '2026-10-10 07:45:00', '2026-10-10 11:00:00', 359.00, 210, 210, 'SCHEDULED', 'system'
WHERE NOT EXISTS (SELECT 1 FROM flights WHERE flight_number='SQ550');

-- Oct 11
INSERT INTO flights (flight_number, airline_id, origin_airport_id, destination_airport_id,
                     departure_time, arrival_time, base_price, total_seats, available_seats, status, created_by)
SELECT 'BA456', (SELECT id FROM airlines WHERE code='BA'),
       (SELECT id FROM airports WHERE iata_code='LHR'), (SELECT id FROM airports WHERE iata_code='SYD'),
       '2026-10-11 13:00:00', '2026-10-12 09:00:00', 1249.00, 220, 220, 'SCHEDULED', 'system'
WHERE NOT EXISTS (SELECT 1 FROM flights WHERE flight_number='BA456');

INSERT INTO flights (flight_number, airline_id, origin_airport_id, destination_airport_id,
                     departure_time, arrival_time, base_price, total_seats, available_seats, status, created_by)
SELECT 'UA410', (SELECT id FROM airlines WHERE code='UA'),
       (SELECT id FROM airports WHERE iata_code='SFO'), (SELECT id FROM airports WHERE iata_code='NRT'),
       '2026-10-11 11:30:00', '2026-10-12 15:00:00', 799.00, 160, 160, 'SCHEDULED', 'system'
WHERE NOT EXISTS (SELECT 1 FROM flights WHERE flight_number='UA410');

INSERT INTO flights (flight_number, airline_id, origin_airport_id, destination_airport_id,
                     departure_time, arrival_time, base_price, total_seats, available_seats, status, created_by)
SELECT 'DL620', (SELECT id FROM airlines WHERE code='DL'),
       (SELECT id FROM airports WHERE iata_code='JFK'), (SELECT id FROM airports WHERE iata_code='FRA'),
       '2026-10-11 17:00:00', '2026-10-12 07:00:00', 719.00, 170, 170, 'SCHEDULED', 'system'
WHERE NOT EXISTS (SELECT 1 FROM flights WHERE flight_number='DL620');

-- Oct 12
INSERT INTO flights (flight_number, airline_id, origin_airport_id, destination_airport_id,
                     departure_time, arrival_time, base_price, total_seats, available_seats, status, created_by)
SELECT 'EK803', (SELECT id FROM airlines WHERE code='EK'),
       (SELECT id FROM airports WHERE iata_code='DXB'), (SELECT id FROM airports WHERE iata_code='SYD'),
       '2026-10-12 03:00:00', '2026-10-12 21:30:00', 949.00, 200, 200, 'SCHEDULED', 'system'
WHERE NOT EXISTS (SELECT 1 FROM flights WHERE flight_number='EK803');

INSERT INTO flights (flight_number, airline_id, origin_airport_id, destination_airport_id,
                     departure_time, arrival_time, base_price, total_seats, available_seats, status, created_by)
SELECT 'AA501', (SELECT id FROM airlines WHERE code='AA'),
       (SELECT id FROM airports WHERE iata_code='MIA'), (SELECT id FROM airports WHERE iata_code='LAX'),
       '2026-10-12 09:00:00', '2026-10-12 13:30:00', 279.00, 180, 180, 'SCHEDULED', 'system'
WHERE NOT EXISTS (SELECT 1 FROM flights WHERE flight_number='AA501');

INSERT INTO flights (flight_number, airline_id, origin_airport_id, destination_airport_id,
                     departure_time, arrival_time, base_price, total_seats, available_seats, status, created_by)
SELECT 'QF102', (SELECT id FROM airlines WHERE code='QF'),
       (SELECT id FROM airports WHERE iata_code='SYD'), (SELECT id FROM airports WHERE iata_code='SIN'),
       '2026-10-12 06:00:00', '2026-10-12 11:30:00', 429.00, 190, 190, 'SCHEDULED', 'system'
WHERE NOT EXISTS (SELECT 1 FROM flights WHERE flight_number='QF102');
