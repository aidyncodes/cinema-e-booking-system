package edu.uga.ces.service;

import edu.uga.ces.dto.TicketLine;
import edu.uga.ces.event.OrderConfirmationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OrderConfirmationEmailServiceTests {

    private JavaMailSender mailSender;
    private OrderConfirmationEmailService service;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        service = new OrderConfirmationEmailService(
                mailSender,
                "no-reply@ces-cinema.test");
    }

    @Test
    void sendsCompleteOrderReceiptWithoutFullCardData() {
        service.sendOrderConfirmation(order());

        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();
        assertEquals("no-reply@ces-cinema.test", message.getFrom());
        assertArrayEquals(
                new String[]{"customer@example.com"},
                message.getTo());
        assertEquals("CES Cinema order CES-20260729-0042", message.getSubject());

        String text = message.getText();
        assertNotNull(text);
        assertTrue(text.contains("Dune: Part Two"));
        assertTrue(text.contains("July 30, 2026"));
        assertTrue(text.contains("7:30 PM"));
        assertTrue(text.contains("Seats: B4, B5"));
        assertTrue(text.contains("Adult x1 @ $12.00 = $12.00"));
        assertTrue(text.contains("Child x1 @ $6.00 = $6.00"));
        assertTrue(text.contains("Total: $19.44"));
        assertTrue(text.contains("Visa ending in 1111"));
        assertTrue(!text.contains("4111111111111111"));
    }

    @Test
    void emailFailureDoesNotFailAlreadyCommittedOrder() {
        doThrow(new MailSendException("SMTP unavailable"))
                .when(mailSender)
                .send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> service.sendOrderConfirmation(order()));
    }

    @Test
    void listenerRunsAfterOrderTransactionCommits() throws Exception {
        TransactionalEventListener listener = OrderConfirmationEmailService.class
                .getMethod("sendOrderConfirmation", OrderConfirmationEvent.class)
                .getAnnotation(TransactionalEventListener.class);

        assertNotNull(listener);
        assertEquals(TransactionPhase.AFTER_COMMIT, listener.phase());
        assertTrue(listener.fallbackExecution());
    }

    private OrderConfirmationEvent order() {
        return new OrderConfirmationEvent(
                "CES-20260729-0042",
                "customer@example.com",
                "Demo",
                "Dune: Part Two",
                LocalDate.of(2026, 7, 30),
                LocalTime.of(19, 30),
                "Auditorium 1",
                List.of("B4", "B5"),
                List.of(
                        new TicketLine("ADULT", 1, 12.00, 12.00),
                        new TicketLine("CHILD", 1, 6.00, 6.00)),
                new BigDecimal("18.00"),
                new BigDecimal("1.44"),
                new BigDecimal("19.44"),
                "Visa",
                "1111");
    }
}
