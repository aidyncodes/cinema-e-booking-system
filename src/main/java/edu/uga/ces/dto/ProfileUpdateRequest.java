package edu.uga.ces.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Email is intentionally absent. The authenticated account's email address
 * cannot be modified through the profile endpoint.
 */
public record ProfileUpdateRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Size(max = 25) String phone,
        @NotNull Boolean promotionsOptIn,
        @Valid AddressRequest address
) {}
