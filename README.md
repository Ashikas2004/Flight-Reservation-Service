# ✈️ SkyBook – Flight Reservation Service

A full-stack flight reservation web application built with **React**, **Spring Boot**, **Hibernate / Spring Data JPA**, and **PostgreSQL**, containerised with **Docker**.

---

## Architecture

```
React Frontend (Nginx)  ──REST/JSON──▶  Spring Boot Backend  ──JPA──▶  PostgreSQL
        :3000                                  :8080                     :5432
```

Based on the layered architecture:

| Layer              | Technology                                 |
|--------------------|--------------------------------------------|
| Client             | React 18, Vite, Tailwind CSS               |
| REST API           | Spring Boot 3.2, Spring MVC                |
| Business Logic     | Spring Service beans, @Transactional       |
| Data Access        | Spring Data JPA, Hibernate, JPQL           |
| Database           | PostgreSQL 16                              |
| Auth               | JWT (JJWT 0.11), Spring Security 6         |
| Migrations         | Flyway                                     |
| Containerisation   | Docker, Docker Compose                     |

---

## Modules

| Module    | Backend                                       | Frontend                         |
|-----------|-----------------------------------------------|----------------------------------|
| **Auth**  | JWT register/login/refresh, BCrypt            | Login, Register pages            |
| **Flight**| Search (paginated), detail, airline/airport   | Flight search & detail pages     |
| **Seat**  | Seat map, lock/release/confirm                | Visual seat picker               |
| **Booking**| Create, confirm, cancel, paginated list      | Booking flow, My Bookings        |
| **Payment**| Mock gateway (95% success), CREDIT_CARD etc | Payment page, success/fail UX    |
| **Refund** | Auto-process via mock gateway               | Refund request page              |
| **Audit** | All key events logged to audit_logs table     | Admin audit log browser          |
| **Admin** | CRUD airlines, airports, flights, seats       | Admin dashboard + 4 CRUD pages   |

---

## Quick Start

### Prerequisites
- Docker Desktop ≥ 4.x
- Docker Compose v2

### Run with Docker Compose

```bash
git clone <repo-url>
cd FlightReservation
cp .env.example .env
# Set DB_PASSWORD and generate a strong JWT_SECRET in .env before starting.
docker compose up --build
```

Services:
- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080/api
- **PostgreSQL**: localhost:5432

### Default Accounts

| Role      | Email                             | Password      |
|-----------|-----------------------------------|---------------|
| Admin     | admin@flightreservation.com       | Admin@1234    |
| Passenger | Register via /register            | your choice   |

---

## Local Development (without Docker)

### Backend

```bash
# Start PostgreSQL (or use Docker just for the DB)
docker run -d -p 5432:5432 \
  -e POSTGRES_DB=flightdb \
  -e POSTGRES_USER=flightuser \
  -e POSTGRES_PASSWORD=flightpass \
  postgres:16-alpine

cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

API available at http://localhost:8080/api

### Frontend

```bash
cd frontend
npm install
npm run dev
```

UI available at http://localhost:5173 (Vite proxies /api → :8080)

---

## API Endpoints

### Auth
| Method | Endpoint              | Description                   | Auth     |
|--------|-----------------------|-------------------------------|----------|
| POST   | /api/auth/register    | Register a new user            | Public   |
| POST   | /api/auth/login       | Login and get JWT tokens        | Public   |
| POST   | /api/auth/refresh     | Refresh access token           | Public   |
| GET    | /api/auth/me          | Current user info              | Bearer   |

### Flights
| Method | Endpoint                    | Description                   | Auth     |
|--------|-----------------------------|-------------------------------|----------|
| GET    | /api/flights/search         | Search flights (paginated)     | Public   |
| GET    | /api/flights/{id}           | Flight details                 | Public   |
| GET    | /api/airlines               | List active airlines           | Public   |
| GET    | /api/airports               | List active airports           | Public   |
| GET    | /api/airports/search?q=     | Search airports                | Public   |

### Seats
| Method | Endpoint                          | Description              | Auth     |
|--------|-----------------------------------|--------------------------|----------|
| GET    | /api/seats/flight/{flightId}      | All seats for a flight   | Public   |
| GET    | /api/seats/flight/{flightId}/available | Available seats    | Public   |
| POST   | /api/seats/lock                   | Lock selected seats       | Bearer   |

### Bookings
| Method | Endpoint                      | Description              | Auth     |
|--------|-------------------------------|--------------------------|----------|
| POST   | /api/bookings                 | Create booking            | Bearer   |
| GET    | /api/bookings/my              | My bookings (paginated)   | Bearer   |
| GET    | /api/bookings/{reference}     | Booking detail            | Bearer   |
| PUT    | /api/bookings/{reference}/cancel | Cancel booking         | Bearer   |

### Payment
| Method | Endpoint                      | Description              | Auth     |
|--------|-------------------------------|--------------------------|----------|
| POST   | /api/payments                 | Process payment           | Bearer   |
| GET    | /api/payments/booking/{id}    | Payment by booking        | Bearer   |

### Refund
| Method | Endpoint                      | Description              | Auth     |
|--------|-------------------------------|--------------------------|----------|
| POST   | /api/refunds                  | Request refund            | Bearer   |
| GET    | /api/refunds/my               | My refunds (paginated)    | Bearer   |
| GET    | /api/refunds/booking/{id}     | Refund by booking         | Bearer   |

### Admin (ROLE_ADMIN only)
```
GET/POST        /api/admin/airlines
PUT/PATCH       /api/admin/airlines/{id}
GET/POST        /api/admin/airports
PUT/PATCH       /api/admin/airports/{id}
POST/PUT/PATCH  /api/admin/flights/{id}
GET/POST/PATCH  /api/admin/seats
GET             /api/audit
GET             /api/audit/entity/{type}/{id}
```

---

## Database Schema

```
users ← bookings → flights → airlines
                          → airports (origin + destination)
bookings → booking_passengers → seats
bookings → payments → refunds
audit_logs (all key events)
```

---

## Booking Flow

```
Search Flights → Select Seats (lock) → Enter Passenger Details
→ Create Booking (PENDING) → Pay → Confirm (CONFIRMED)
          ↓
      Cancel (CANCELLED) → Request Refund → REFUNDED
```

---

## Environment Variables (Backend)

| Variable                  | Default                          | Description           |
|---------------------------|----------------------------------|-----------------------|
| DB_HOST                   | localhost                        | Postgres host         |
| DB_PORT                   | 5432                             | Postgres port         |
| DB_NAME                   | flightdb                         | Database name         |
| DB_USER                   | flightuser                       | DB username           |
| DB_PASSWORD               | Required (set in `.env`)         | DB password           |
| JWT_SECRET                | Required (set in `.env`)         | JWT signing key       |
| JWT_EXPIRATION_MS         | 86400000 (24h)                   | Access token TTL      |
| JWT_REFRESH_EXPIRATION_MS | 604800000 (7d)                   | Refresh token TTL     |
| SERVER_PORT               | 8080                             | Server port           |
| CORS_ALLOWED_ORIGINS      | http://localhost:3000,...         | Allowed CORS origins  |

---

## Technology Decisions

- **Flyway**: Schema versioned migrations (`V1__init_schema.sql`, `V2__seed_data.sql`)
- **BCrypt**: Password hashing with strength 12
- **JWT HS256**: Stateless auth, no session state
- **Seat locking**: Optimistic locking via `UPDATE WHERE status = 'AVAILABLE'` to prevent double-booking
- **Mock Payment Gateway**: Simulates 95% success rate; swap `MockPaymentGateway` for real Stripe/PayPal SDK
- **Audit logging**: Every booking, payment, refund, and seat event is persisted to `audit_logs`
- **Multi-stage Docker builds**: Smaller production images (JRE + Nginx only)
