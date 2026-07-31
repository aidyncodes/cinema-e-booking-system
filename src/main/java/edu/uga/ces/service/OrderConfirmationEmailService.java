package edu.uga.ces.service;

import edu.uga.ces.dto.TicketLine;
import edu.uga.ces.event.OrderConfirmationEvent;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Sends the purchase receipt only after the order transaction commits.
 *
 * The order-finalization service integrates by publishing an
 * OrderConfirmationEvent through ApplicationEventPublisher.
 */
@Service
public class OrderConfirmationEmailService {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("MMMM d, uuuu", Locale.US);
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("h:mm a", Locale.US);

    private final OrderEmailGateway emailGateway;

    public OrderConfirmationEmailService(OrderEmailGateway emailGateway) {
        this.emailGateway = emailGateway;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true)
    public void sendOrderConfirmation(OrderConfirmationEvent order) {
        try {
            emailGateway.send(
                    order.confirmationEmail(),
                    "CES Cinema order " + order.confirmationNumber(),
                    buildMessage(order));
        } catch (MailException ex) {
            // The purchase is already committed. Email failure must not turn a
            // successful paid order into an error response or duplicate order.
            System.err.println("Could not send order confirmation email: " + ex.getMessage());
        }
    }

    private String buildMessage(OrderConfirmationEvent order) {
        StringBuilder body = new StringBuilder();
        body.append("Hi ").append(order.customerFirstName()).append(",\n\n")
                .append("Your CES Cinema order is confirmed.\n\n")
                .append("Confirmation: ").append(order.confirmationNumber()).append('\n')
                .append("Movie: ").append(order.movieTitle()).append('\n')
                .append("Date: ").append(order.showDate().format(DATE_FORMAT)).append('\n')
                .append("Time: ").append(order.showTime().format(TIME_FORMAT)).append('\n')
                .append("Showroom: ").append(order.showroom()).append('\n')
                .append("Seats: ").append(String.join(", ", order.seats())).append("\n\n")
                .append("Tickets:\n");

        for (TicketLine ticket : order.tickets()) {
            body.append("- ")
                    .append(ticketLabel(ticket.type()))
                    .append(" x")
                    .append(ticket.count())
                    .append(" @ ")
                    .append(money(BigDecimal.valueOf(ticket.pricePerTicket())))
                    .append(" = ")
                    .append(money(BigDecimal.valueOf(ticket.lineTotal())))
                    .append('\n');
        }

        body.append("\nSubtotal: ").append(money(order.subtotal()))
                .append("\nTax: ").append(money(order.taxAmount()))
                .append("\nTotal: ").append(money(order.totalAmount()))
                .append("\nPayment: ").append(order.paymentCardBrand())
                .append(" ending in ").append(order.paymentCardLastFour())
                .append("\n\nThank you for choosing CES Cinema.\n");
        return body.toString();
    }

    private String ticketLabel(String type) {
        String normalized = type == null ? "" : type.trim().toLowerCase(Locale.US);
        if (normalized.isEmpty()) {
            return "Ticket";
        }
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }

    private String money(BigDecimal value) {
        return "$" + value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
