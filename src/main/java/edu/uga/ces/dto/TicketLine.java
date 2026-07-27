package edu.uga.ces.dto;

/** One age-category line on the order summary. */
public record TicketLine(
        String type,
        int count,
        double pricePerTicket,
        double lineTotal
) {
}
