package edu.uga.ces.exception;

/** Thrown when someone requests a showtime id that does not exist. */
public class ShowtimeNotFoundException extends RuntimeException {
    public ShowtimeNotFoundException(Long id) {
        super("No showtime with id " + id);
    }
}
