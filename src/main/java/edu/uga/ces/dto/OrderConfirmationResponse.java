package edu.uga.ces.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Returned to the frontend once an order has been finalized: seats are
 * BOOKED, the card has been recorded, and the confirmation email has been
 * queued (sent after the transaction commits).
 */
public record OrderConfirmationResponse(
        String confirmationNumber,
        String movieTitle,
        LocalDate showDate,
        LocalTime showTime,
        String showroomName,
        List<String> seats,
        List<TicketLine> tickets,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String paymentCardBrand,
        String paymentCardLastFour
) {
}