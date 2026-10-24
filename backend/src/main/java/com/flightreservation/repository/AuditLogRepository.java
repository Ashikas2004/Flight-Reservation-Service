package com.flightreservation.repository;

import com.flightreservation.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId, Pageable pageable);

    Page<AuditLog> findByPerformedBy(String performedBy, Pageable pageable);

    Page<AuditLog> findByAction(AuditLog.AuditAction action, Pageable pageable);

    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:entityType IS NULL OR a.entityType = :entityType)
              AND a.action IN :allowedActions
              AND (:action IS NULL OR a.action = :action)
              AND (:performedBy IS NULL OR a.performedBy = :performedBy)
              AND a.createdAt BETWEEN :from AND :to
            ORDER BY a.createdAt DESC
            """)
    Page<AuditLog> findWithFilters(
            @Param("entityType") String entityType,
            @Param("action") AuditLog.AuditAction action,
            @Param("allowedActions") java.util.Collection<AuditLog.AuditAction> allowedActions,
            @Param("performedBy") String performedBy,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    List<AuditLog> findByEntityTypeAndEntityIdAndActionInOrderByCreatedAtDesc(
            String entityType, Long entityId, java.util.Collection<AuditLog.AuditAction> allowedActions);
}
