package edu.uga.ces.service;

/**
 * Factory Method creator contract. Each concrete factory creates the pricing
 * product for one ticket category.
 */
public interface TicketPricingFactory {
    String supportedTicketType();
    TicketPrice createTicketPrice();
}
