package edu.uga.ces.repository;

import edu.uga.ces.model.SeatReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatReservationRepository extends JpaRepository<SeatReservation, Long> {

    // All seats already held or booked for a showtime - used to build the seat map.
    List<SeatReservation> findByShowtimeId(Long showtimeId);

    // The current session's held seats, used for the checkout order summary.
    List<SeatReservation> findBySessionIdAndStatus(String sessionId, String status);

    // Clears a session's existing holds before re-holding a fresh selection.
    // A guest books one showtime at a time, so we drop all of their held seats.
    long deleteBySessionIdAndStatus(String sessionId, String status);
}
