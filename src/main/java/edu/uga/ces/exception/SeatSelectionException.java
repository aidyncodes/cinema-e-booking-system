package edu.uga.ces.exception;

/**
 * Thrown for invalid seat selections: the seat count not matching the ticket
 * count, or a seat label that is not part of the showroom.
 */
public class SeatSelectionException extends RuntimeException {
    public SeatSelectionException(String message) {
        super(message);
    }
}
