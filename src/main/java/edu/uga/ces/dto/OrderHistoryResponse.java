package edu.uga.ces.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * One past order for the logged-in user's order history: the confirmation
 * number, the show it was for, the seats/tickets bought, and the amounts paid.
 */
public record OrderHistoryResponse(
        String confirmationNumber,
        String status,
        String movieTitle,
        LocalDate showDate,
        LocalTime showTime,
        String showroomName,
        List<OrderTicketResponse> tickets,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String paymentCardBrand,
        String paymentCardLastFour,
        Instant placedAt
) {
}
