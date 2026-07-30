package edu.uga.ces.dto;

import java.math.BigDecimal;

/** One seat/ticket line shown under an order in the history view. */
public record OrderTicketResponse(
        String seatLabel,
        String ticketType,
        BigDecimal unitPrice
) {
}
