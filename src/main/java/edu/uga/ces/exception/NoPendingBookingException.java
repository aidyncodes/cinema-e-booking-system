package edu.uga.ces.exception;

/** Thrown when a checkout summary is requested but the session holds no seats. */
public class NoPendingBookingException extends RuntimeException {
    public NoPendingBookingException() {
        super("No seats are being held for this session. Select a showtime and seats first.");
    }
}
