package edu.uga.ces.exception;

/** Thrown when someone registers with an email that's already taken. */
public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String email) {
        super("An account with email " + email + " already exists");
    }
}