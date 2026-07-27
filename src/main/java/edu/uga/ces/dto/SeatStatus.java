package edu.uga.ces.dto;

/**
 * One unavailable seat on the seat map. "mine" is true when the seat is held by
 * the current session, so the frontend can pre-select it instead of greying it out.
 */
public record SeatStatus(
        String seatLabel,
        String status,
        boolean mine
) {
}
