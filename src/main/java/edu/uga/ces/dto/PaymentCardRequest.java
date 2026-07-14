package edu.uga.ces.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PaymentCardRequest(
        @NotBlank @Size(max = 150) String cardholderName,
        String cardNumber,
        @Min(1) @Max(12) int expirationMonth,
        @Min(2000) int expirationYear
) {}
