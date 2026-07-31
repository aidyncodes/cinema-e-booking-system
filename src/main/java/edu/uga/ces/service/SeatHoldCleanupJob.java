package edu.uga.ces.service;

import edu.uga.ces.model.SeatReservation;
import edu.uga.ces.repository.SeatReservationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Releases seat holds nobody finished checking out on.
 */
@Component
public class SeatHoldCleanupJob {

    private final SeatReservationRepository seatReservationRepository;

    public SeatHoldCleanupJob(SeatReservationRepository seatReservationRepository) {
        this.seatReservationRepository = seatReservationRepository;
    }

    // Sweeps once a minute. fixedDelay (not fixedRate) so a slow run can't
    // overlap the next one; initialDelay so it doesn't race app startup.
    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    @Transactional
    public void releaseExpiredHolds() {
        seatReservationRepository.deleteByStatusAndExpiresAtBefore(
                SeatReservation.STATUS_HELD, Instant.now());
    }
}