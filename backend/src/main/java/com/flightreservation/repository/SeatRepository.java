package com.flightreservation.repository;

import com.flightreservation.entity.Seat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByFlightId(Long flightId);

    List<Seat> findByFlightIdAndStatus(Long flightId, Seat.SeatStatus status);

    List<Seat> findByFlightIdAndSeatClassAndStatus(Long flightId, Seat.SeatClass seatClass, Seat.SeatStatus status);

    Optional<Seat> findByFlightIdAndSeatNumber(Long flightId, String seatNumber);

    @Query("SELECT COUNT(s) FROM Seat s WHERE s.flight.id = :flightId AND s.status = :status")
    long countByFlightIdAndStatus(@Param("flightId") Long flightId, @Param("status") Seat.SeatStatus status);

    /**
     * Pessimistic write lock — acquires SELECT ... FOR UPDATE on the rows.
     * Blocks any concurrent transaction from reading or updating these seats
     * until the current transaction commits or rolls back.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.id IN :ids")
    List<Seat> findAllByIdsForUpdate(@Param("ids") List<Long> ids);

    /**
     * Atomic conditional UPDATE: only changes status if current status matches.
     * Returns the number of rows actually updated.
     * This is the core concurrency guard — if 2 users attempt the same seat
     * simultaneously only one UPDATE will match (the other finds status already changed).
     */
    @Modifying
    @Query("""
            UPDATE Seat s
            SET    s.status   = :newStatus,
                   s.lockedAt = :lockedAt,
                   s.lockedBy = :lockedBy
            WHERE  s.id IN :ids
              AND  s.status = :currentStatus
            """)
    int lockSeatsAtomically(
            @Param("ids")           List<Long> ids,
            @Param("currentStatus") Seat.SeatStatus currentStatus,
            @Param("newStatus")     Seat.SeatStatus newStatus,
            @Param("lockedAt")      LocalDateTime lockedAt,
            @Param("lockedBy")      String lockedBy);

    /**
     * Locks seats for user if they are AVAILABLE or already LOCKED by this same user.
     * Returns number of rows updated.
     */
    @Modifying
    @Query("""
            UPDATE Seat s
            SET    s.status   = :lockedStatus,
                   s.lockedAt = :lockedAt,
                   s.lockedBy = :lockedBy
            WHERE  s.id IN :ids
              AND  (s.status = :availableStatus OR (s.status = :lockedStatus AND s.lockedBy = :lockedBy))
            """)
    int lockSeatsForUser(
            @Param("ids")             List<Long> ids,
            @Param("availableStatus") Seat.SeatStatus availableStatus,
            @Param("lockedStatus")    Seat.SeatStatus lockedStatus,
            @Param("lockedAt")        LocalDateTime lockedAt,
            @Param("lockedBy")        String lockedBy);

    /**
     * Releases any seats on the flight that were locked by this user,
     * EXCEPT the seats that the user wants to keep.
     */
    @Modifying
    @Query("""
            UPDATE Seat s
            SET    s.status   = :availableStatus,
                   s.lockedAt = null,
                   s.lockedBy = null
            WHERE  s.flight.id = :flightId
              AND  s.lockedBy  = :user
              AND  s.status    = :lockedStatus
              AND  s.id NOT IN :keepSeatIds
            """)
    int releaseOtherSeatsLockedByUser(
            @Param("flightId")        Long flightId,
            @Param("user")            String user,
            @Param("lockedStatus")    Seat.SeatStatus lockedStatus,
            @Param("availableStatus") Seat.SeatStatus availableStatus,
            @Param("keepSeatIds")     List<Long> keepSeatIds);

    @Modifying
    @Query("""
            UPDATE Seat s
            SET    s.status   = :availableStatus,
                   s.lockedAt = null,
                   s.lockedBy = null
            WHERE  s.id IN :ids
              AND  s.status   = :lockedStatus
              AND  s.lockedBy = :user
            """)
    int releaseSeatsForUser(
            @Param("ids")             List<Long> ids,
            @Param("user")            String user,
            @Param("lockedStatus")    Seat.SeatStatus lockedStatus,
            @Param("availableStatus") Seat.SeatStatus availableStatus);

    @Modifying
    @Query("""
            UPDATE Seat s
            SET    s.status   = :availableStatus,
                   s.lockedAt = null,
                   s.lockedBy = null
            WHERE  s.id IN :ids
              AND  s.status   = :lockedStatus
            """)
    int releaseSeatsAdmin(
            @Param("ids")             List<Long> ids,
            @Param("lockedStatus")    Seat.SeatStatus lockedStatus,
            @Param("availableStatus") Seat.SeatStatus availableStatus);

    /** Simple status-only update used for confirm/release operations */
    @Modifying
    @Query("UPDATE Seat s SET s.status = :newStatus WHERE s.id IN :ids AND s.status = :currentStatus")
    int updateStatusByIdsAndCurrentStatus(
            @Param("ids")           List<Long> ids,
            @Param("currentStatus") Seat.SeatStatus currentStatus,
            @Param("newStatus")     Seat.SeatStatus newStatus);

    @Query("SELECT s FROM Seat s WHERE s.id IN :ids")
    List<Seat> findAllByIds(@Param("ids") List<Long> ids);

    /**
     * Releases expired locks — seats locked before the expiry time
     * that are still LOCKED (never confirmed into a booking) are reset to AVAILABLE.
     */
    @Modifying
    @Query("""
            UPDATE Seat s
            SET    s.status   = 'AVAILABLE',
                   s.lockedAt = null,
                   s.lockedBy = null
            WHERE  s.status   = 'LOCKED'
              AND  s.lockedAt < :expiryTime
            """)
    int releaseExpiredLocks(@Param("expiryTime") LocalDateTime expiryTime);
}
