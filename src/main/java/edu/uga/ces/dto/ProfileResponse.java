package edu.uga.ces.dto;

import java.util.List;

public record ProfileResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        String phone,
        Boolean promotionsOptIn,
        AddressResponse address,
        List<PaymentCardResponse> paymentCards
) {}
