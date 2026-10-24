package com.flightreservation.controller;

import com.flightreservation.dto.response.AuditLogResponse;
import com.flightreservation.dto.response.PageResponse;
import com.flightreservation.exception.GlobalExceptionHandler;
import com.flightreservation.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuditControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuditController auditController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(auditController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private AuditLogResponse createMockAuditLog(Long id, String action, String entityType, Long entityId) {
        return AuditLogResponse.builder()
                .id(id)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .performedBy("admin@flightreservation.com")
                .description("Action performed on entity")
                .ipAddress("127.0.0.1")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("GET /audit")
    class GetAuditLogsTests {

        @Test
        @DisplayName("Success - returns paginated audit logs with filters")
        void getAuditLogs_Success() throws Exception {
            AuditLogResponse log1 = createMockAuditLog(1L, "BOOKING_CONFIRMED", "Booking", 100L);
            PageResponse<AuditLogResponse> page = PageResponse.<AuditLogResponse>builder()
                    .content(List.of(log1))
                    .pageNumber(0)
                    .pageSize(20)
                    .totalElements(1)
                    .totalPages(1)
                    .last(true)
                    .build();

            when(auditService.getAuditLogs(eq("Booking"), eq("BOOKING_CONFIRMED"), eq("admin@flightreservation.com"),
                    isNull(), isNull(), eq(0), eq(20))).thenReturn(page);

            mockMvc.perform(get("/audit")
                            .param("entityType", "Booking")
                            .param("action", "BOOKING_CONFIRMED")
                            .param("performedBy", "admin@flightreservation.com")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].action", is("BOOKING_CONFIRMED")))
                    .andExpect(jsonPath("$.content[0].entityType", is("Booking")));

            verify(auditService, times(1)).getAuditLogs(eq("Booking"), eq("BOOKING_CONFIRMED"), eq("admin@flightreservation.com"),
                    isNull(), isNull(), eq(0), eq(20));
        }

        @Test
        @DisplayName("Success - handles date filters in ISO format")
        void getAuditLogs_WithDateFilters() throws Exception {
            PageResponse<AuditLogResponse> page = PageResponse.<AuditLogResponse>builder()
                    .content(Collections.emptyList())
                    .pageNumber(0)
                    .pageSize(20)
                    .totalElements(0)
                    .totalPages(0)
                    .last(true)
                    .build();

            when(auditService.getAuditLogs(isNull(), isNull(), isNull(),
                    any(LocalDateTime.class), any(LocalDateTime.class), eq(0), eq(20))).thenReturn(page);

            mockMvc.perform(get("/audit")
                            .param("from", "2026-01-01T00:00:00")
                            .param("to", "2026-12-31T23:59:59"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /audit/entity/{entityType}/{entityId}")
    class GetByEntityTests {

        @Test
        @DisplayName("Success - returns list of audit logs for specific entity")
        void getByEntity_Success() throws Exception {
            AuditLogResponse log1 = createMockAuditLog(1L, "BOOKING_CONFIRMED", "Booking", 100L);
            when(auditService.getAuditLogsByEntity("Booking", 100L)).thenReturn(List.of(log1));

            mockMvc.perform(get("/audit/entity/{entityType}/{entityId}", "Booking", 100L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].entityId", is(100)))
                    .andExpect(jsonPath("$[0].action", is("BOOKING_CONFIRMED")));

            verify(auditService, times(1)).getAuditLogsByEntity("Booking", 100L);
        }

        @Test
        @DisplayName("Success - returns empty list when no audit logs found for entity")
        void getByEntity_Empty_Returns200() throws Exception {
            when(auditService.getAuditLogsByEntity("Flight", 999L)).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/audit/entity/{entityType}/{entityId}", "Flight", 999L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }
}
