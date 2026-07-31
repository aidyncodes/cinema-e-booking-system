package edu.uga.ces.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class SeniorTicketPricingFactory implements TicketPricingFactory {
    @Override
    public String supportedTicketType() { return "SENIOR"; }

    @Override
    public TicketPrice createTicketPrice() {
        return new TicketPrice("SENIOR", new BigDecimal("8.00"));
    }
}
