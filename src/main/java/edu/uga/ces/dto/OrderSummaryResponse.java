package edu.uga.ces.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Checkout order summary for the seats the current session is holding.
 * Total is before tax and fees, which are out of scope for this sprint.
 */
public record OrderSummaryResponse(
        Long showtimeId,
        String movieTitle,
        LocalDate showDate,
        LocalTime showTime,
        String showroomName,
        List<String> seats,
        List<TicketLine> tickets,
        int totalTickets,
        double totalBeforeTax
) {
}
