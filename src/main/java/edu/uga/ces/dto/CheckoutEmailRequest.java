package edu.uga.ces.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Confirmation email selected on the checkout summary page. */
public record CheckoutEmailRequest(
        @NotBlank @Email @Size(max = 255) String confirmationEmail
) {
}
