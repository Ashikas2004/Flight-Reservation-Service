-- ============================================================
-- V1: Initial schema for Flight Reservation Service
-- ============================================================

-- Users
CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(100) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    first_name  VARCHAR(100) NOT NULL,
    last_name   VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20),
    role        VARCHAR(20)  NOT NULL DEFAULT 'PASSENGER',
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP,
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100)
);

-- Airlines
CREATE TABLE airlines (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(10)  NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    country     VARCHAR(100),
    logo_url    VARCHAR(255),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP,
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100)
);

-- Airports
CREATE TABLE airports (
    id          BIGSERIAL PRIMARY KEY,
    iata_code   VARCHAR(3)   NOT NULL UNIQUE,
    name        VARCHAR(150) NOT NULL,
    city        VARCHAR(100) NOT NULL,
    country     VARCHAR(100) NOT NULL,
    timezone    VARCHAR(50),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP,
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100)
);

-- Flights
CREATE TABLE flights (
    id                      BIGSERIAL PRIMARY KEY,
    flight_number           VARCHAR(20)    NOT NULL UNIQUE,
    airline_id              BIGINT         NOT NULL REFERENCES airlines(id),
    origin_airport_id       BIGINT         NOT NULL REFERENCES airports(id),
    destination_airport_id  BIGINT         NOT NULL REFERENCES airports(id),
    departure_time          TIMESTAMP      NOT NULL,
    arrival_time            TIMESTAMP      NOT NULL,
    base_price              NUMERIC(10, 2) NOT NULL,
    total_seats             INT            NOT NULL,
    available_seats         INT            NOT NULL,
    status                  VARCHAR(20)    NOT NULL DEFAULT 'SCHEDULED',
    created_at              TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP,
    created_by              VARCHAR(100),
    updated_by              VARCHAR(100)
);
CREATE INDEX idx_flight_origin_dest    ON flights (origin_airport_id, destination_airport_id);
CREATE INDEX idx_flight_departure      ON flights (departure_time);
CREATE INDEX idx_flight_number         ON flights (flight_number);

-- Seats
CREATE TABLE seats (
    id          BIGSERIAL PRIMARY KEY,
    flight_id   BIGINT         NOT NULL REFERENCES flights(id),
    seat_number VARCHAR(5)     NOT NULL,
    seat_class  VARCHAR(20)    NOT NULL,
    status      VARCHAR(20)    NOT NULL DEFAULT 'AVAILABLE',
    extra_price NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    created_at  TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP,
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100),
    CONSTRAINT uk_seat_flight_number UNIQUE (flight_id, seat_number)
);

-- Bookings
CREATE TABLE bookings (
    id                  BIGSERIAL PRIMARY KEY,
    booking_reference   VARCHAR(10)    NOT NULL UNIQUE,
    user_id             BIGINT         NOT NULL REFERENCES users(id),
    flight_id           BIGINT         NOT NULL REFERENCES flights(id),
    status              VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    total_amount        NUMERIC(10, 2) NOT NULL,
    booked_at           TIMESTAMP,
    cancelled_at        TIMESTAMP,
    cancellation_reason TEXT,
    created_at          TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100)
);
CREATE INDEX idx_booking_user      ON bookings (user_id);
CREATE INDEX idx_booking_reference ON bookings (booking_reference);
CREATE INDEX idx_booking_flight    ON bookings (flight_id);

-- Booking Passengers
CREATE TABLE booking_passengers (
    id              BIGSERIAL PRIMARY KEY,
    booking_id      BIGINT       NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    seat_id         BIGINT       NOT NULL REFERENCES seats(id),
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    passport_number VARCHAR(50),
    date_of_birth   VARCHAR(20),
    nationality     VARCHAR(60),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);

-- Payments
CREATE TABLE payments (
    id              BIGSERIAL PRIMARY KEY,
    booking_id      BIGINT         NOT NULL UNIQUE REFERENCES bookings(id),
    transaction_id  VARCHAR(100)   UNIQUE,
    amount          NUMERIC(10, 2) NOT NULL,
    currency        VARCHAR(3)     NOT NULL DEFAULT 'USD',
    payment_method  VARCHAR(30)    NOT NULL,
    status          VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    gateway_response TEXT,
    paid_at         TIMESTAMP,
    failed_reason   VARCHAR(255),
    created_at      TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);
CREATE INDEX idx_payment_booking     ON payments (booking_id);
CREATE INDEX idx_payment_transaction ON payments (transaction_id);

-- Refunds
CREATE TABLE refunds (
    id                      BIGSERIAL PRIMARY KEY,
    booking_id              BIGINT         NOT NULL UNIQUE REFERENCES bookings(id),
    payment_id              BIGINT         NOT NULL UNIQUE REFERENCES payments(id),
    refund_amount           NUMERIC(10, 2) NOT NULL,
    currency                VARCHAR(3)     NOT NULL DEFAULT 'USD',
    status                  VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    reason                  TEXT,
    refund_transaction_id   VARCHAR(100)   UNIQUE,
    processed_at            TIMESTAMP,
    created_at              TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP,
    created_by              VARCHAR(100),
    updated_by              VARCHAR(100)
);
CREATE INDEX idx_refund_booking ON refunds (booking_id);
CREATE INDEX idx_refund_payment ON refunds (payment_id);

-- Audit Logs
CREATE TABLE audit_logs (
    id           BIGSERIAL PRIMARY KEY,
    action       VARCHAR(30)  NOT NULL,
    entity_type  VARCHAR(50)  NOT NULL,
    entity_id    BIGINT,
    performed_by VARCHAR(100),
    description  TEXT,
    old_value    TEXT,
    new_value    TEXT,
    ip_address   VARCHAR(50),
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_audit_entity  ON audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_user    ON audit_logs (performed_by);
CREATE INDEX idx_audit_action  ON audit_logs (action);
CREATE INDEX idx_audit_created ON audit_logs (created_at);
