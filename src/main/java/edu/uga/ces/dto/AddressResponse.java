package edu.uga.ces.dto;

public record AddressResponse(
        Long id,
        String streetLine1,
        String streetLine2,
        String city,
        String state,
        String postalCode,
        String country
) {}
