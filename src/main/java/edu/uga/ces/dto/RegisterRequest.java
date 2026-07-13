package edu.uga.ces.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body for POST /api/auth/register.
 * No "role" field on purpose, role is always assigned server-side (CUSTOMER),
 * never taken from client input. Otherwise anyone could register as ADMIN.
 */
public record RegisterRequest(
        @NotBlank @Email String email,
        // BCrypt silently ignores/truncates bytes past 72, so we cap it here instead
        // of letting that surprise someone later.
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank String firstName,
        @NotBlank String lastName,
        // Optional, the shared schema allows a null phone.
        String phone,
        Boolean promotionsOptIn
) {}