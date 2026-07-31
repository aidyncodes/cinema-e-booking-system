package edu.uga.ces.repository;

import edu.uga.ces.model.SeatReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.List;

public interface SeatReservationRepository extends JpaRepository<SeatReservation, Long> {

    // All seats already held or booked for a showtime - used to build the seat map.
    List<SeatReservation> findByShowtimeId(Long showtimeId);

    // The current session's held seats, used for the checkout order summary.
    List<SeatReservation> findBySessionIdAndStatus(String sessionId, String status);

    // Clears a session's existing holds before re-holding a fresh selection.
    // A guest books one showtime at a time, so we drop all of their held seats.
    long deleteBySessionIdAndStatus(String sessionId, String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select reservation
            from SeatReservation reservation
            where reservation.sessionId = :sessionId and reservation.status = :status
            order by reservation.id
            """)
    List<SeatReservation> findHeldBySessionIdForUpdate(
            @Param("sessionId") String sessionId,
            @Param("status") String status);

    // Used by SeatHoldCleanupJob to release seats nobody finished checking out
    // on. Only ever matches HELD rows, since BOOKED seats are removed by the
    // checkout flow itself once they become permanent tickets.
    long deleteByStatusAndExpiresAtBefore(String status, Instant cutoff);
}