package edu.uga.ces.dto;

public record PaymentCardResponse(
        Long id,
        String cardholderName,
        String cardBrand,
        String lastFour,
        int expirationMonth,
        int expirationYear
) {}
