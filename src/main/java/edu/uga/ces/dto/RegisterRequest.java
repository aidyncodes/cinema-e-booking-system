package edu.uga.ces.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body for POST /api/auth/register.
 * No "role" field since role is always assigned on server side (CUSTOMER),
 * never taken from client input. Otherwise anyone could register as ADMIN.
 */
public record RegisterRequest(
        @NotBlank @Email String email,
        // BCrypt truncates bytes past 72, so cap here to give heads up
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String phone,
        Boolean promotionsOptIn
) {}