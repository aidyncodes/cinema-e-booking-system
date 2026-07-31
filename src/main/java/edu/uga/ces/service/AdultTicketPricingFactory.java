package edu.uga.ces.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AdultTicketPricingFactory implements TicketPricingFactory {
    @Override
    public String supportedTicketType() { return "ADULT"; }

    @Override
    public TicketPrice createTicketPrice() {
        return new TicketPrice("ADULT", new BigDecimal("12.00"));
    }
}
