package edu.uga.ces.exception;

/** Thrown when someone requests a movie id that does not exist. */
public class MovieNotFoundException extends RuntimeException {
    public MovieNotFoundException(Long id) {
        super("No movie with id " + id);
    }
}