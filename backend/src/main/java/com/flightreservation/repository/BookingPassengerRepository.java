package com.flightreservation.repository;

import com.flightreservation.entity.BookingPassenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingPassengerRepository extends JpaRepository<BookingPassenger, Long> {

    @Query("SELECT bp FROM BookingPassenger bp JOIN FETCH bp.seat WHERE bp.booking.id = :bookingId")
    List<BookingPassenger> findByBookingIdWithSeat(@Param("bookingId") Long bookingId);
}
