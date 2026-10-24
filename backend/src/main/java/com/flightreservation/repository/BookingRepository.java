package com.flightreservation.repository;

import com.flightreservation.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingReference(String bookingReference);

    boolean existsByBookingReference(String bookingReference);

    @Query(value = """
            SELECT b FROM Booking b
            JOIN FETCH b.flight f
            JOIN FETCH f.originAirport
            JOIN FETCH f.destinationAirport
            JOIN FETCH f.airline
            WHERE b.user.id = :userId
              AND (b.status IN ('CONFIRMED', 'REFUNDED')
                   OR EXISTS (SELECT p FROM Payment p WHERE p.booking.id = b.id AND p.status IN ('SUCCESS', 'REFUNDED')))
            ORDER BY b.createdAt DESC
            """,
            countQuery = """
            SELECT COUNT(b) FROM Booking b
            WHERE b.user.id = :userId
              AND (b.status IN ('CONFIRMED', 'REFUNDED')
                   OR EXISTS (SELECT p FROM Payment p WHERE p.booking.id = b.id AND p.status IN ('SUCCESS', 'REFUNDED')))
            """)
    Page<Booking> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            SELECT DISTINCT b FROM Booking b
            JOIN FETCH b.flight f
            JOIN FETCH f.originAirport
            JOIN FETCH f.destinationAirport
            JOIN FETCH f.airline
            JOIN FETCH b.user
            LEFT JOIN FETCH b.passengers bp
            LEFT JOIN FETCH bp.seat
            WHERE b.bookingReference = :reference
            """)
    Optional<Booking> findByReferenceWithDetails(@Param("reference") String reference);

    @Query("""
            SELECT DISTINCT b FROM Booking b
            JOIN FETCH b.flight f
            JOIN FETCH f.originAirport
            JOIN FETCH f.destinationAirport
            JOIN FETCH f.airline
            JOIN FETCH b.user
            LEFT JOIN FETCH b.passengers bp
            LEFT JOIN FETCH bp.seat
            WHERE b.id = :id
            """)
    Optional<Booking> findByIdWithDetails(@Param("id") Long id);

    @Query("""
            SELECT b FROM Booking b
            WHERE b.flight.id = :flightId
              AND b.status NOT IN ('CANCELLED', 'REFUNDED')
            """)
    List<Booking> findActiveBookingsByFlightId(@Param("flightId") Long flightId);

    @Query("""
            SELECT DISTINCT b FROM Booking b
            JOIN FETCH b.user
            LEFT JOIN FETCH b.passengers bp
            LEFT JOIN FETCH bp.seat
            WHERE b.flight.id = :flightId
              AND b.status = 'CONFIRMED'
            """)
    List<Booking> findConfirmedBookingsByFlightId(@Param("flightId") Long flightId);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.user.id = :userId AND b.status = :status")
    long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Booking.BookingStatus status);

    @Query("""
            SELECT b FROM Booking b
            WHERE b.flight.id = :flightId
              AND b.user.id = :userId
              AND b.status = 'PENDING'
            """)
    List<Booking> findPendingBookingsByUserAndFlight(
            @Param("flightId") Long flightId,
            @Param("userId")   Long userId);

    @Query("""
            SELECT DISTINCT b FROM Booking b
            LEFT JOIN FETCH b.passengers bp
            LEFT JOIN FETCH bp.seat
            JOIN FETCH b.user
            JOIN FETCH b.flight
            WHERE b.id = :id
            """)
    Optional<Booking> findByIdWithPassengers(@Param("id") Long id);
}
