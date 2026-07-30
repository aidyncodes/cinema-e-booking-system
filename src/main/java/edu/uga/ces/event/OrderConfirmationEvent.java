package edu.uga.ces.event;

import edu.uga.ces.dto.TicketLine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Complete, immutable order details published by the order-finalization
 * transaction. OrderConfirmationEmailService listens after that transaction
 * commits, so a failed purchase cannot produce a success email.
 */
public record OrderConfirmationEvent(
        String confirmationNumber,
        String confirmationEmail,
        String customerFirstName,
        String movieTitle,
        LocalDate showDate,
        LocalTime showTime,
        String showroom,
        List<String> seats,
        List<TicketLine> tickets,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String paymentCardBrand,
        String paymentCardLastFour
) {
    public OrderConfirmationEvent {
        seats = List.copyOf(seats);
        tickets = List.copyOf(tickets);
    }
}
