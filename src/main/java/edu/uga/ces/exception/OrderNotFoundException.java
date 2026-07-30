package edu.uga.ces.exception;

/** Thrown when an order is absent or does not belong to the logged-in user. */
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(String confirmationNumber) {
        super("Order " + confirmationNumber + " was not found.");
    }
}
