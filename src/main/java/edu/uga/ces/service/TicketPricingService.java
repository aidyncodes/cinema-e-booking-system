package edu.uga.ces.service;

import edu.uga.ces.exception.SeatSelectionException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Selects a concrete Factory Method creator for the requested ticket type. */
@Service
public class TicketPricingService {

    private final Map<String, TicketPricingFactory> factories;

    public TicketPricingService(List<TicketPricingFactory> factories) {
        this.factories = factories.stream().collect(Collectors.toUnmodifiableMap(
                factory -> factory.supportedTicketType().toUpperCase(Locale.US),
                Function.identity()));
    }

    public BigDecimal priceFor(String ticketType) {
        String normalized = ticketType == null ? "" : ticketType.trim().toUpperCase(Locale.US);
        TicketPricingFactory factory = factories.get(normalized);
        if (factory == null) {
            throw new SeatSelectionException("Unsupported ticket type: " + ticketType);
        }
        return factory.createTicketPrice().amount();
    }
}
