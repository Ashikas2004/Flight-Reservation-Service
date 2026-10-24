package com.flightreservation.service;

import com.flightreservation.dto.response.AuditLogResponse;
import com.flightreservation.dto.response.PageResponse;
import com.flightreservation.entity.AuditLog;
import com.flightreservation.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditService auditService;

    private AuditLog testLog;

    @BeforeEach
    void setUp() {
        testLog = AuditLog.builder()
                .id(1L)
                .action(AuditLog.AuditAction.BOOKING_CONFIRMED)
                .entityType("Booking")
                .entityId(100L)
                .performedBy("user@example.com")
                .description("Booking confirmed")
                .ipAddress("127.0.0.1")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("getAuditLogs - returns filtered audit logs")
    void getAuditLogs_ReturnsFilteredLogs() {
        Page<AuditLog> page = new PageImpl<>(List.of(testLog));
        when(auditLogRepository.findWithFilters(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<AuditLogResponse> response = auditService.getAuditLogs(
                "Booking", "BOOKING_CONFIRMED", "user@example.com",
                LocalDateTime.now().minusDays(1), LocalDateTime.now(), 0, 10
        );

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals("BOOKING_CONFIRMED", response.getContent().get(0).getAction());
    }

    @Test
    @DisplayName("getAuditLogs - returns empty page when action is not in displayable list")
    void getAuditLogs_NonDisplayableAction_ReturnsEmptyPage() {
        PageResponse<AuditLogResponse> response = auditService.getAuditLogs(
                "Booking", "USER_LOGIN", "user@example.com",
                null, null, 0, 10
        );

        assertNotNull(response);
        assertTrue(response.getContent().isEmpty());
        verify(auditLogRepository, never()).findWithFilters(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("getAuditLogsByEntity - returns entity specific audit logs")
    void getAuditLogsByEntity_ReturnsLogs() {
        when(auditLogRepository.findByEntityTypeAndEntityIdAndActionInOrderByCreatedAtDesc(
                eq("Booking"), eq(100L), eq(AuditService.DISPLAYABLE_ACTIONS)))
                .thenReturn(List.of(testLog));

        List<AuditLogResponse> result = auditService.getAuditLogsByEntity("Booking", 100L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("BOOKING_CONFIRMED", result.get(0).getAction());
    }

    @Test
    @DisplayName("getAuditLogs - null action, blank action, and invalid action enum")
    void getAuditLogs_ActionBranches() {
        Page<AuditLog> page = new PageImpl<>(List.of(testLog));
        when(auditLogRepository.findWithFilters(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        // action is null, from/to are null
        PageResponse<AuditLogResponse> res1 = auditService.getAuditLogs("Booking", null, null, null, null, 0, 10);
        assertNotNull(res1);

        // action is blank
        PageResponse<AuditLogResponse> res2 = auditService.getAuditLogs("Booking", "  ", null, null, null, 0, 10);
        assertNotNull(res2);

        // action throws IllegalArgumentException on enum parsing
        PageResponse<AuditLogResponse> res3 = auditService.getAuditLogs("Booking", "NOT_A_VALID_ACTION", null, null, null, 0, 10);
        assertNotNull(res3);
    }
}

