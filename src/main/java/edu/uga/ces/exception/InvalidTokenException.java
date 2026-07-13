package edu.uga.ces.exception;

/** Thrown when a confirmation link's token doesn't exist or has expired. */
public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) {
        super(message);
    }
}