package edu.uga.ces.dto;

/**
 * Returned on successful login.
 */
public record AuthResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        String role
) {}