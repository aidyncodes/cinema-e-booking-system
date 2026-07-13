package edu.uga.ces.exception;

/** Thrown when someone with correct credentials tries to log in before confirming their email. */
public class AccountNotActiveException extends RuntimeException {
    public AccountNotActiveException(String message) {
        super(message);
    }
}