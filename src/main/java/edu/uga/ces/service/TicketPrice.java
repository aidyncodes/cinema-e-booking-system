package edu.uga.ces.service;

import java.math.BigDecimal;

/** Product created by a ticket-pricing factory method. */
public record TicketPrice(String ticketType, BigDecimal amount) {
}
