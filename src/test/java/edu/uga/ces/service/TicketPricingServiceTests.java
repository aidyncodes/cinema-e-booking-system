package edu.uga.ces.service;

import edu.uga.ces.exception.SeatSelectionException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TicketPricingServiceTests {

    private final TicketPricingService service = new TicketPricingService(List.of(
            new AdultTicketPricingFactory(),
            new SeniorTicketPricingFactory(),
            new ChildTicketPricingFactory()));

    @Test
    void concreteFactoryMethodsCreatePriceForEachTicketType() {
        assertEquals(new BigDecimal("12.00"), service.priceFor("adult"));
        assertEquals(new BigDecimal("8.00"), service.priceFor("SENIOR"));
        assertEquals(new BigDecimal("6.00"), service.priceFor(" child "));
    }

    @Test
    void rejectsUnsupportedTicketType() {
        assertThrows(SeatSelectionException.class, () -> service.priceFor("STUDENT"));
    }
}
