package com.flightreservation.controller;

import com.flightreservation.config.SecurityConfig;
import com.flightreservation.dto.response.PageResponse;
import com.flightreservation.repository.FlightRepository;
import com.flightreservation.repository.SeatRepository;
import com.flightreservation.security.JwtAuthenticationEntryPoint;
import com.flightreservation.security.JwtAuthenticationFilter;
import com.flightreservation.security.JwtTokenProvider;
import com.flightreservation.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        AdminAirlineController.class,
        AdminAirportController.class,
        AdminFlightController.class,
        AuditController.class,
        BookingController.class,
        FlightController.class,
        AirportController.class,
        AirlineController.class,
        PaymentController.class,
        RefundController.class,
        SeatController.class
})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class})
@TestPropertySource(properties = {
        "app.cors.allowed-origins=http://localhost:3000"
})
class SecurityAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserDetailsService userDetailsService;

    // Service Mocks
    @MockBean
    private AirlineService airlineService;

    @MockBean
    private AirportService airportService;

    @MockBean
    private FlightService flightService;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private RefundService refundService;

    @MockBean
    private SeatService seatService;

    @MockBean
    private AuditService auditService;

    @MockBean
    private SeatRepository seatRepository;

    @MockBean
    private FlightRepository flightRepository;

    @MockBean
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMappingContext;

    @Nested
    @DisplayName("Public Endpoints - Accessible without authentication")
    class PublicEndpointsTests {

        @Test
        @DisplayName("GET /airlines - permits anonymous access")
        void getAirlines_AnonymousAllowed() throws Exception {
            when(airlineService.getAllActive()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/airlines"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /airports - permits anonymous access")
        void getAirports_AnonymousAllowed() throws Exception {
            when(airportService.getAll()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/airports"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /flights/search - permits anonymous access")
        void searchFlights_AnonymousAllowed() throws Exception {
            PageResponse<com.flightreservation.dto.response.FlightResponse> page = PageResponse.<com.flightreservation.dto.response.FlightResponse>builder()
                    .content(Collections.emptyList())
                    .build();
            when(flightService.searchFlights(any())).thenReturn(page);

            mockMvc.perform(get("/flights/search")
                            .param("origin", "JFK")
                            .param("destination", "LAX")
                            .param("departureDate", "2026-12-01"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Protected Passenger Endpoints - Require authentication")
    class ProtectedEndpointsTests {

        @Test
        @DisplayName("GET /bookings/my - returns 401 when unauthenticated")
        void getMyBookings_Unauthenticated_Returns401() throws Exception {
            mockMvc.perform(get("/bookings/my"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /bookings/my - returns 200 when authenticated as PASSENGER")
        @WithMockUser(username = "passenger@test.com", roles = "PASSENGER")
        void getMyBookings_Authenticated_Returns200() throws Exception {
            PageResponse<com.flightreservation.dto.response.BookingResponse> page = PageResponse.<com.flightreservation.dto.response.BookingResponse>builder()
                    .content(Collections.emptyList())
                    .build();
            when(bookingService.getMyBookings(anyInt(), anyInt())).thenReturn(page);

            mockMvc.perform(get("/bookings/my"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /bookings - returns 401 when unauthenticated")
        void createBooking_Unauthenticated_Returns401() throws Exception {
            mockMvc.perform(post("/bookings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("POST /payments - returns 401 when unauthenticated")
        void processPayment_Unauthenticated_Returns401() throws Exception {
            mockMvc.perform(post("/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("POST /refunds - returns 401 when unauthenticated")
        void requestRefund_Unauthenticated_Returns401() throws Exception {
            mockMvc.perform(post("/refunds")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Admin Endpoints - Require ROLE_ADMIN")
    class AdminEndpointsTests {

        @Test
        @DisplayName("GET /admin/airlines - returns 401 when unauthenticated")
        void getAdminAirlines_Unauthenticated_Returns401() throws Exception {
            mockMvc.perform(get("/admin/airlines"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /admin/airlines - returns 403 when authenticated as PASSENGER")
        @WithMockUser(username = "passenger@test.com", roles = "PASSENGER")
        void getAdminAirlines_PassengerRole_Returns403() throws Exception {
            mockMvc.perform(get("/admin/airlines"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /admin/airlines - returns 200 when authenticated as ADMIN")
        @WithMockUser(username = "admin@test.com", roles = "ADMIN")
        void getAdminAirlines_AdminRole_Returns200() throws Exception {
            when(airlineService.getAll()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/admin/airlines"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /admin/airports - returns 403 when authenticated as PASSENGER")
        @WithMockUser(username = "passenger@test.com", roles = "PASSENGER")
        void getAdminAirports_PassengerRole_Returns403() throws Exception {
            mockMvc.perform(get("/admin/airports"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /admin/airports - returns 200 when authenticated as ADMIN")
        @WithMockUser(username = "admin@test.com", roles = "ADMIN")
        void getAdminAirports_AdminRole_Returns200() throws Exception {
            when(airportService.getAll()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/admin/airports"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /admin/flights - returns 403 when authenticated as PASSENGER")
        @WithMockUser(username = "passenger@test.com", roles = "PASSENGER")
        void getAdminFlights_PassengerRole_Returns403() throws Exception {
            mockMvc.perform(get("/admin/flights"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /admin/flights - returns 200 when authenticated as ADMIN")
        @WithMockUser(username = "admin@test.com", roles = "ADMIN")
        void getAdminFlights_AdminRole_Returns200() throws Exception {
            PageResponse<com.flightreservation.dto.response.FlightResponse> page = PageResponse.<com.flightreservation.dto.response.FlightResponse>builder()
                    .content(Collections.emptyList())
                    .build();
            when(flightService.getAllFlights(anyInt(), anyInt())).thenReturn(page);

            mockMvc.perform(get("/admin/flights"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /audit - returns 403 when authenticated as PASSENGER")
        @WithMockUser(username = "passenger@test.com", roles = "PASSENGER")
        void getAudit_PassengerRole_Returns403() throws Exception {
            mockMvc.perform(get("/audit"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /audit - returns 200 when authenticated as ADMIN")
        @WithMockUser(username = "admin@test.com", roles = "ADMIN")
        void getAudit_AdminRole_Returns200() throws Exception {
            PageResponse<com.flightreservation.dto.response.AuditLogResponse> page = PageResponse.<com.flightreservation.dto.response.AuditLogResponse>builder()
                    .content(Collections.emptyList())
                    .build();
            when(auditService.getAuditLogs(any(), any(), any(), any(), any(), anyInt(), anyInt())).thenReturn(page);

            mockMvc.perform(get("/audit"))
                    .andExpect(status().isOk());
        }
    }
}

