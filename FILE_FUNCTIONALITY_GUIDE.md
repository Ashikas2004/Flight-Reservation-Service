# FlightReservation File Functionality Guide

This document explains the purpose of the main files and folders in the FlightReservation project.

## 1) Root project files

### README.md
- Project overview for the application.
- Includes architecture, setup instructions, quick start, environment variables, API structure, and feature summary.

### docker-compose.yml
- Starts the application services together in Docker.
- Usually runs the backend, frontend, and PostgreSQL database for local development.

### .env.example
- Sample environment variables for configuration.
- Used to create the actual `.env` file without committing secrets.

### .gitignore
- Excludes local environment and generated files from Git.

### .idea/
- IDE project configuration files for JetBrains IntelliJ/IDEA.

---

## 2) Backend project structure

Path: `backend/src/main/java/com/flightreservation`

### FlightReservationApplication.java
- Main Spring Boot entry point.
- Starts the application, enables JPA auditing, and scheduling support.

### config/

#### SecurityConfig.java
- Central Spring Security configuration.
- Defines public vs protected routes.
- Enables JWT-based authentication.
- Configures CORS for frontend access.
- Configures BCrypt password hashing and authentication manager.

#### AuditingConfig.java
- Provides the current logged-in username for audit fields.
- If no authenticated user exists, it falls back to `system`.

### controller/

#### AuthController.java
- Handles login, registration, refresh token, and current-user endpoints.

#### AirlineController.java
- Public API for viewing active airlines.

#### AirportController.java
- Public API for viewing airports and searching by location/name.

#### FlightController.java
- Public API for searching flights and fetching flight details.

#### SeatController.java
- Handles seat listing, availability checks, seat locking, and seat release.

#### BookingController.java
- Handles passenger booking creation and retrieving booking details.

#### PaymentController.java
- Handles payment requests and payment lookup related to bookings.

#### RefundController.java
- Handles refund requests and refund status retrieval.

#### AuditController.java
- Exposes audit logs for administrators.

#### AdminAirlineController.java
- Admin-only CRUD operations for airlines.

#### AdminAirportController.java
- Admin-only CRUD operations for airports.

#### AdminFlightController.java
- Admin-only flight management endpoints.

#### AdminSeatController.java
- Admin-only seat management operations.

#### AdminBookingController.java
- Admin-only booking management and review endpoints.

### dto/

#### request/
- `AirlineRequest.java` – DTO for airline create/update request data.
- `AirlineUpdateRequest.java` – DTO for partial airline updates.
- `AirportRequest.java` – DTO for airport creation/update.
- `BookingRequest.java` – DTO for creating a booking.
- `CancelBookingRequest.java` – DTO for booking cancellation requests.
- `FlightRequest.java` – DTO for flight creation/update.
- `FlightSearchRequest.java` – DTO for flight search filters.
- `LoginRequest.java` – DTO for login payload.
- `PassengerRequest.java` – DTO for passenger details.
- `PaymentRequest.java` – DTO for payment details.
- `RefundRequest.java` – DTO for refund request input.
- `RegisterRequest.java` – DTO for user registration.
- `SeatLockRequest.java` – DTO for locking seat(s).
- `SeatReleaseRequest.java` – DTO for releasing seat locks.
- `SeatRequest.java` – DTO for seat-related actions.

#### response/
- `AirlineResponse.java` – Airline output model.
- `AirportResponse.java` – Airport output model.
- `AuditLogResponse.java` – Audit log representation for UI/admin views.
- `AuthResponse.java` – Returned data after login/register, including token info.
- `BookingResponse.java` – Booking summary and detail output.
- `FlightResponse.java` – Flight output model.
- `PageResponse.java` – Generic paginated API response wrapper.
- `PaymentResponse.java` – Payment result response.
- `RefundResponse.java` – Refund response model.
- `SeatResponse.java` – Seat availability and seat data response.

### entity/
These are JPA entities that map to database tables.

- `User.java` – Application user account and roles.
- `Airline.java` – Airline company metadata.
- `Airport.java` – Airport information.
- `Flight.java` – Flight route and schedule data.
- `Seat.java` – Seat record for each flight.
- `Booking.java` – Booking record and status details.
- `BookingPassenger.java` – Passenger information tied to a booking.
- `Payment.java` – Payment transaction details.
- `Refund.java` – Refund information.
- `AuditLog.java` – Stored audit trail for important actions.
- `BaseEntity.java` – Shared base entity with common audit fields.

### exception/

#### ApiException.java
- Custom exception for backend domain errors.

#### GlobalExceptionHandler.java
- Handles API exceptions and returns consistent HTTP error responses.

### repository/
These interfaces provide database access with Spring Data JPA.

- `UserRepository.java` – User lookups and authentication data.
- `AirlineRepository.java` – Airline queries.
- `AirportRepository.java` – Airport queries.
- `FlightRepository.java` – Flight search and booking-related queries.
- `SeatRepository.java` – Seat data lookups and availability logic.
- `BookingRepository.java` – Booking retrieval and status queries.
- `BookingPassengerRepository.java` – Passenger records for bookings.
- `PaymentRepository.java` – Payment query access.
- `RefundRepository.java` – Refund queries.
- `AuditLogRepository.java` – Audit log persistence and retrieval.

### service/
Business logic layer for the application.

- `AuthService.java` – User registration, login, token generation, and refresh logic.
- `AirlineService.java` – Airline CRUD and validation.
- `AirportService.java` – Airport data logic.
- `FlightService.java` – Flight search, listing, and details.
- `SeatService.java` – Seat availability, locking, and booking seat management.
- `BookingService.java` – Booking creation, confirmation, cancellation, and status updates.
- `PaymentService.java` – Payment processing and validation.
- `MockPaymentGateway.java` – Simulated payment processing service used for mock payment flows.
- `RefundService.java` – Refund request creation and processing.
- `AuditService.java` – Writes audit logs for system actions.

### security/

#### JwtAuthenticationFilter.java
- Intercepts incoming HTTP requests.
- Reads and validates JWT tokens.
- Adds the authenticated principal to the Spring Security context.

#### JwtAuthenticationEntryPoint.java
- Handles unauthenticated access attempts.
- Returns a proper 401 response for invalid or missing tokens.

#### JwtProperties.java
- Stores JWT configuration values used for signing and validation.

#### JwtTokenProvider.java
- Generates and validates JWT tokens.

#### UserDetailsServiceImpl.java
- Loads user details from the database for Spring Security authentication.

---

## 3) Frontend project structure

Path: `frontend/src`

### App.jsx
- Main application router.
- Declares public routes, protected routes, and admin-only routes.
- Wraps app with ThemeProvider and AuthProvider.

### main.jsx
- Mounts the React app into the browser root.

### context/

#### AuthContext.jsx
- Stores current authenticated user state.
- Provides login/logout and token management logic.

#### ThemeContext.jsx
- Manages application theme (light/dark mode) state.

### api/

#### axiosInstance.js
- Shared Axios instance for backend API communication.
- Sets base URL and common headers.

#### authApi.js
- Calls auth-related API endpoints.

#### flightApi.js
- Calls flight search and flight detail endpoints.

#### seatApi.js
- Calls seat and seat lock/release endpoints.

#### bookingApi.js
- Calls booking creation and booking history endpoints.

#### paymentApi.js
- Calls payment-related endpoints.

#### refundApi.js
- Calls refund endpoints.

#### adminApi.js
- Admin-specific API calls for airline, airport, flight, seat, and audit management.

### components/

#### common/
- `Navbar.jsx` – Top navigation bar with login/logout and navigation links.
- `ProtectedRoute.jsx` – Restricts access to authenticated users.
- `Alert.jsx` – Reusable alert message component.
- `Spinner.jsx` – Loading indicator.
- `StatusBadge.jsx` – Displays booking/status labels with colors.

#### booking/
- `CommonTravelInfo.jsx` – Displays general travel guidelines for passengers.
- `ClassFareBenefits.jsx` – Shows class-based benefits such as baggage, meals, and seat perks.

### pages/

#### HomePage.jsx
- Landing page for the booking application.

#### auth/
- `LoginPage.jsx` – User login form.
- `RegisterPage.jsx` – User registration form.

#### flights/
- `FlightSearchPage.jsx` – Search flights by route and date.
- `FlightDetailPage.jsx` – Shows detailed information about a selected flight.

#### booking/
- `SeatSelectionPage.jsx` – Lets users choose available seats.
- `PassengerDetailsPage.jsx` – Collects passenger info before booking.
- `BookingConfirmPage.jsx` – Final confirmation step before payment.

#### payment/
- `PaymentPage.jsx` – Payment form and confirmation UI.

#### bookings/
- `MyBookingsPage.jsx` – Shows all bookings for the logged-in user.
- `BookingDetailPage.jsx` – Detailed booking information page.
- `RefundPage.jsx` – Allows users to request refunds.

#### admin/
- `AdminDashboard.jsx` – Admin landing dashboard.
- `AdminAirlinesPage.jsx` – Manage airlines.
- `AdminAirportsPage.jsx` – Manage airports.
- `AdminFlightsPage.jsx` – Manage flights.
- `AdminSeatsPage.jsx` – Manage seats for a specific flight.
- `AdminAuditPage.jsx` – View audit logs and system events.

### hooks/
- Custom React hooks used for reuse and cleaner component logic.

### utils/
- Utility functions used across the frontend.

---

## 4) Database migration files

Path: `backend/src/main/resources/db/migration`

- `V1__init_schema.sql` – Creates the initial database schema.
- `V2__seed_data.sql` – Inserts initial seed data such as airlines, airports, flights, and admin user data.
- `V3__fix_admin_password.sql` – Updates the default admin account password/hash.
- `V4__add_flights_oct_2026.sql` – Adds additional flight records for October 2026.
- `V5__generate_seats_for_seed_flights.sql` – Generates seat rows for seeded flight data.
- `V6__seat_lock_tracking.sql` – Adds table/logic for tracking seat lock status.

---

## 5) Application behavior summary

The project is structured as a typical full-stack booking system:

- Frontend handles user experience and route-based views.
- Backend exposes REST APIs and business logic.
- JPA entities map to the database.
- Services enforce validation and business workflows.
- Security config protects endpoints with JWT and role-based access.
- Admin pages allow operational management of flights, airports, airlines, seats, and audit information.

The `CommonTravelInfo.jsx` component is a small but important frontend feature that shows common passenger travel guidance before or during the booking flow.
