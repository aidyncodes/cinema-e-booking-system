package edu.uga.ces.exception;

/** Thrown on login when the email isn't found or the password doesn't match. */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
