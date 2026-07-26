package edu.uga.ces.exception;

public class ShowroomNotFoundException extends RuntimeException {

    public ShowroomNotFoundException(Long id) {
        super("Showroom not found with id: " + id);
    }
}
