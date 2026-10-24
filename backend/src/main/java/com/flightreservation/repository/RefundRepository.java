package com.flightreservation.repository;

import com.flightreservation.entity.Refund;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {

    Optional<Refund> findByBookingId(Long bookingId);

    Optional<Refund> findByRefundTransactionId(String refundTransactionId);

    @Query("""
            SELECT r FROM Refund r
            JOIN FETCH r.booking b
            JOIN FETCH b.user u
            JOIN FETCH r.payment
            WHERE u.id = :userId
            ORDER BY r.createdAt DESC
            """)
    Page<Refund> findByUserId(@Param("userId") Long userId, Pageable pageable);

    boolean existsByBookingId(Long bookingId);
}
