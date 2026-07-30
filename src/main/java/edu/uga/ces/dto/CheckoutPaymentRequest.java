package edu.uga.ces.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Payment details supplied when a held-seat booking is finalized.
 *
 * The frontend sends only the saved-card id. The backend must load that card
 * for the authenticated user instead of accepting card brand or last-four
 * values from the browser.
 */
public record CheckoutPaymentRequest(
        @NotNull @Positive Long paymentCardId,
        @NotBlank @Email @Size(max = 255) String confirmationEmail
) {}
