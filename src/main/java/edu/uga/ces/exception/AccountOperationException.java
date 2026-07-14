package edu.uga.ces.exception;

public class AccountOperationException extends RuntimeException {
    public AccountOperationException(String message) {
        super(message);
    }
}
