package edu.uga.ces.exception;

/** Thrown when a logged-in but non-admin user hits an admin-only endpoint. */
public class AdminAccessRequiredException extends RuntimeException {
    public AdminAccessRequiredException() {
        super("This action requires an administrator account.");
    }
}