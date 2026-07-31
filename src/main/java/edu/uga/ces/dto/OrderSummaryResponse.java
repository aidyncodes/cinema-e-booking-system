package edu.uga.ces.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Checkout order summary for the seats the current session is holding.
 * Amounts are calculated by the backend so checkout never trusts browser totals.
 */
public record OrderSummaryResponse(
        Long showtimeId,
        String movieTitle,
        String posterUrl,
        LocalDate showDate,
        LocalTime showTime,
        String showroomName,
        List<String> seats,
        List<TicketLine> tickets,
        int totalTickets,
        double totalBeforeTax,
        double taxAmount,
        double totalAmount
) {
}
