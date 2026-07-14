package edu.uga.ces.exception;

public class AuthenticationRequiredException extends RuntimeException {
    public AuthenticationRequiredException() {
        super("You must be logged in to perform this action.");
    }
}
