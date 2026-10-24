package com.flightreservation.service;

import com.flightreservation.dto.response.AuditLogResponse;
import com.flightreservation.dto.response.PageResponse;
import com.flightreservation.entity.AuditLog;
import com.flightreservation.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {

    public static final List<AuditLog.AuditAction> DISPLAYABLE_ACTIONS = List.of(
            AuditLog.AuditAction.BOOKING_CONFIRMED,
            AuditLog.AuditAction.BOOKING_CANCELLED,
            AuditLog.AuditAction.PAYMENT_SUCCESS,
            AuditLog.AuditAction.REFUND_PROCESSED,
            AuditLog.AuditAction.REFUND_FAILED,
            AuditLog.AuditAction.FLIGHT_CREATED,
            AuditLog.AuditAction.FLIGHT_UPDATED,
            AuditLog.AuditAction.FLIGHT_CANCELLED,
            AuditLog.AuditAction.USER_REGISTERED
    );

    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> getAuditLogs(
            String entityType, String action, String performedBy,
            LocalDateTime from, LocalDateTime to, int page, int size) {

        AuditLog.AuditAction auditAction = null;
        if (action != null && !action.isBlank()) {
            try {
                AuditLog.AuditAction requested = AuditLog.AuditAction.valueOf(action.toUpperCase());
                if (DISPLAYABLE_ACTIONS.contains(requested)) {
                    auditAction = requested;
                } else {
                    return PageResponse.of(org.springframework.data.domain.Page.empty());
                }
            } catch (IllegalArgumentException ignored) {}
        }

        LocalDateTime fromTime = (from != null) ? from : LocalDateTime.now().minusDays(30);
        LocalDateTime toTime = (to != null) ? to : LocalDateTime.now();

        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return PageResponse.of(auditLogRepository.findWithFilters(
                entityType, auditAction, DISPLAYABLE_ACTIONS, performedBy, fromTime, toTime, pageable
        ).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAuditLogsByEntity(String entityType, Long entityId) {
        return auditLogRepository.findByEntityTypeAndEntityIdAndActionInOrderByCreatedAtDesc(entityType, entityId, DISPLAYABLE_ACTIONS)
                .stream().map(this::toResponse).toList();
    }

    private AuditLogResponse toResponse(AuditLog a) {
        return AuditLogResponse.builder()
                .id(a.getId())
                .action(a.getAction().name())
                .entityType(a.getEntityType())
                .entityId(a.getEntityId())
                .performedBy(a.getPerformedBy())
                .description(a.getDescription())
                .ipAddress(a.getIpAddress())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
