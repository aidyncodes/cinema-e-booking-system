package edu.uga.ces.exception;

import java.util.List;

/** Thrown when one or more requested seats are already held or booked by someone else. */
public class SeatUnavailableException extends RuntimeException {
    public SeatUnavailableException(List<String> seats) {
        super("These seats are no longer available: " + String.join(", ", seats));
    }
}
