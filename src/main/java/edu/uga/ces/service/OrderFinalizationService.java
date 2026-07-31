package edu.uga.ces.service;

import edu.uga.ces.dto.CheckoutPaymentRequest;
import edu.uga.ces.dto.OrderConfirmationResponse;
import edu.uga.ces.dto.PaymentCardSnapshot;
import edu.uga.ces.dto.TicketLine;
import edu.uga.ces.event.OrderConfirmationEvent;
import edu.uga.ces.exception.AuthenticationRequiredException;
import edu.uga.ces.exception.NoPendingBookingException;
import edu.uga.ces.exception.SeatUnavailableException;
import edu.uga.ces.exception.ShowtimeNotFoundException;
import edu.uga.ces.model.Order;
import edu.uga.ces.model.PaymentTransaction;
import edu.uga.ces.model.SeatReservation;
import edu.uga.ces.model.Showtime;
import edu.uga.ces.model.Ticket;
import edu.uga.ces.model.User;
import edu.uga.ces.repository.OrderRepository;
import edu.uga.ces.repository.PaymentTransactionRepository;
import edu.uga.ces.repository.SeatReservationRepository;
import edu.uga.ces.repository.ShowtimeRepository;
import edu.uga.ces.repository.TicketRepository;
import edu.uga.ces.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finalizes checkout for the seats this session currently holds.
 *   1. Resolves and validates the buyer's saved card (CheckoutPaymentService).
 *   2. Writes the permanent record required by the checkout contract in
 *      README_DB_SETUP.md: inserts the Order row, one Ticket row per
 *      purchased seat, and an APPROVED PaymentTransaction row, then deletes
 *      the temporary SeatReservation holds.
 *   3. Publishes an OrderConfirmationEvent so OrderConfirmationEmailService
 *      sends the receipt only after this transaction commits.
 *
 * Now this method creates an Order, Ticket, or PaymentTransaction row
 *
 * NOTE: this service is not connected to any controller
 */
@Service
public class OrderFinalizationService {

    // Flat sales-tax rate. 
    private static final BigDecimal TAX_RATE = new BigDecimal("0.08");

    // Same ticket prices BookingService/CheckoutOrderService use.
    private static final Map<String, BigDecimal> PRICES = Map.of(
            "ADULT", new BigDecimal("12.00"),
            "SENIOR", new BigDecimal("8.00"),
            "CHILD", new BigDecimal("6.00")
    );
    private static final List<String> TICKET_TYPES = List.of("ADULT", "SENIOR", "CHILD");

    // Avoids ambiguous characters in the printed confirmation code.
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final Pattern SEAT_LABEL = Pattern.compile("^([A-Z])(\\d+)$");

    private final SeatReservationRepository seatReservationRepository;
    private final ShowtimeRepository showtimeRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final CheckoutPaymentService checkoutPaymentService;
    private final ApplicationEventPublisher eventPublisher;

    public OrderFinalizationService(SeatReservationRepository seatReservationRepository,
                                    ShowtimeRepository showtimeRepository,
                                    UserRepository userRepository,
                                    OrderRepository orderRepository,
                                    TicketRepository ticketRepository,
                                    PaymentTransactionRepository paymentTransactionRepository,
                                    CheckoutPaymentService checkoutPaymentService,
                                    ApplicationEventPublisher eventPublisher) {
        this.seatReservationRepository = seatReservationRepository;
        this.showtimeRepository = showtimeRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.ticketRepository = ticketRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.checkoutPaymentService = checkoutPaymentService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public OrderConfirmationResponse confirmOrder(String sessionId, Long userId, CheckoutPaymentRequest request) {
        // Locks the held rows for the rest of this transaction 
        List<SeatReservation> held = seatReservationRepository
                .findHeldBySessionIdForUpdate(sessionId, SeatReservation.STATUS_HELD);
        if (held.isEmpty()) {
            throw new NoPendingBookingException();
        }

        // All held seats are supposed to belong to the same showtime
        Long showtimeId = held.get(0).getShowtimeId();
        if (held.stream().anyMatch(reservation -> !showtimeId.equals(reservation.getShowtimeId()))) {
            throw new NoPendingBookingException();
        }
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ShowtimeNotFoundException(showtimeId));

        User buyer = userRepository.findById(userId)
                .orElseThrow(AuthenticationRequiredException::new);

        // Validates the card belongs to this user and hasn't expired; throws AccountOperationException otherwise.
        PaymentCardSnapshot card = checkoutPaymentService.requireSavedCard(userId, request.paymentCardId());

        List<String> seats = held.stream()
                .map(SeatReservation::getSeatLabel)
                .sorted(OrderFinalizationService::compareSeatLabels)
                .toList();

        Map<String, Integer> counts = new LinkedHashMap<>();
        TICKET_TYPES.forEach(type -> counts.put(type, 0));
        for (SeatReservation reservation : held) {
            counts.merge(reservation.getTicketType(), 1, Integer::sum);
        }

        List<TicketLine> lines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (String type : TICKET_TYPES) {
            int count = counts.getOrDefault(type, 0);
            if (count == 0) {
                continue;
            }
            BigDecimal price = PRICES.getOrDefault(type, BigDecimal.ZERO);
            BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(count));
            subtotal = subtotal.add(lineTotal);
            lines.add(new TicketLine(type, count, price.doubleValue(), lineTotal.doubleValue()));
        }
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal tax = subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(tax);

        Instant placedAt = Instant.now();
        String confirmationNumber = generateConfirmationNumber();

        // 1. Insert the orders row.
        Order order = new Order();
        order.setConfirmationNumber(confirmationNumber);
        order.setUserId(userId);
        order.setShowtimeId(showtimeId);
        order.setStatus(Order.STATUS_PAID);
        order.setSubtotal(subtotal);
        order.setTaxAmount(tax);
        order.setTotalAmount(total);
        order.setConfirmationEmail(request.confirmationEmail().trim());
        order.setPaymentCardBrand(card.cardBrand());
        order.setPaymentCardLastFour(card.lastFour());
        order.setPlacedAt(placedAt);
        // Flush now so the generated id is available for the ticket rows below.
        order = orderRepository.saveAndFlush(order);

        // 2. Insert one tickets row per purchased seat.
        List<Ticket> tickets = new ArrayList<>();
        for (SeatReservation reservation : held) {
            Ticket ticket = new Ticket();
            ticket.setOrderId(order.getId());
            ticket.setShowtimeId(showtimeId);
            ticket.setSeatLabel(reservation.getSeatLabel());
            ticket.setTicketType(reservation.getTicketType());
            ticket.setUnitPrice(PRICES.getOrDefault(reservation.getTicketType(), BigDecimal.ZERO));
            tickets.add(ticket);
        }
        try {
            ticketRepository.saveAllAndFlush(tickets);
        } catch (DataIntegrityViolationException ex) {
            throw new SeatUnavailableException(seats);
        }

        // 3. Insert an APPROVED payment_transactions row.
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setOrderId(order.getId());
        transaction.setStatus(PaymentTransaction.STATUS_APPROVED);
        transaction.setAmount(total);
        transaction.setTransactionReference("DEMO-" + confirmationNumber);
        transaction.setCardBrand(card.cardBrand());
        transaction.setCardLastFour(card.lastFour());
        transaction.setProcessedAt(placedAt);
        paymentTransactionRepository.save(transaction);

        // 4. Once tickets exist, they are the permanent sold-seat records, so the temporary holds are fully deleted
        seatReservationRepository.deleteAll(held);

        // Listener runs after this transaction commits, so an email can never go out if a purchase failed to save.
        eventPublisher.publishEvent(new OrderConfirmationEvent(
                confirmationNumber,
                order.getConfirmationEmail(),
                buyer.getFirstName(),
                showtime.getMovie().getTitle(),
                showtime.getShowDate(),
                showtime.getShowTime(),
                showtime.getShowroom().getName(),
                seats,
                lines,
                subtotal,
                tax,
                total,
                card.cardBrand(),
                card.lastFour()
        ));

        return new OrderConfirmationResponse(
                confirmationNumber,
                showtime.getMovie().getTitle(),
                showtime.getShowDate(),
                showtime.getShowTime(),
                showtime.getShowroom().getName(),
                seats,
                lines,
                subtotal,
                tax,
                total,
                card.cardBrand(),
                card.lastFour()
        );
    }

    private String generateConfirmationNumber() {
        StringBuilder code = new StringBuilder("CES-");
        for (int i = 0; i < 6; i++) {
            code.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        return code.toString();
    }

    // Same seat ordering BookingService/CheckoutOrderService use. row letter, then seat number (so A2 sorts before A10, not after it).
    private static int compareSeatLabels(String left, String right) {
        Matcher leftMatch = SEAT_LABEL.matcher(left);
        Matcher rightMatch = SEAT_LABEL.matcher(right);
        if (!leftMatch.matches() || !rightMatch.matches()) {
            return left.compareTo(right);
        }
        int rowCompare = leftMatch.group(1).compareTo(rightMatch.group(1));
        if (rowCompare != 0) {
            return rowCompare;
        }
        return Integer.compare(
                Integer.parseInt(leftMatch.group(2)),
                Integer.parseInt(rightMatch.group(2)));
    }
}