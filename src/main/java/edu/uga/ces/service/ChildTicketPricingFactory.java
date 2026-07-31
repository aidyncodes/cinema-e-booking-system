package edu.uga.ces.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ChildTicketPricingFactory implements TicketPricingFactory {
    @Override
    public String supportedTicketType() { return "CHILD"; }

    @Override
    public TicketPrice createTicketPrice() {
        return new TicketPrice("CHILD", new BigDecimal("6.00"));
    }
}
