package com.flightreservation.repository;

import com.flightreservation.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByBookingId(Long bookingId);

    Optional<Payment> findByTransactionId(String transactionId);

    @Query("""
            SELECT p FROM Payment p
            JOIN FETCH p.booking b
            JOIN FETCH b.user
            WHERE p.booking.id = :bookingId
            """)
    Optional<Payment> findByBookingIdWithDetails(@Param("bookingId") Long bookingId);

    boolean existsByBookingId(Long bookingId);
}
