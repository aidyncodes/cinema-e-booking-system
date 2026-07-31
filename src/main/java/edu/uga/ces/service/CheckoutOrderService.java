package edu.uga.ces.service;

import edu.uga.ces.dto.CheckoutPaymentRequest;
import edu.uga.ces.dto.OrderHistoryResponse;
import edu.uga.ces.dto.OrderTicketResponse;
import edu.uga.ces.dto.PaymentCardSnapshot;
import edu.uga.ces.dto.TicketLine;
import edu.uga.ces.event.OrderConfirmationEvent;
import edu.uga.ces.exception.NoPendingBookingException;
import edu.uga.ces.exception.SeatUnavailableException;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Converts the current session's seat hold into a paid order atomically. */
@Service
public class CheckoutOrderService {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.08");

    private final CheckoutPaymentService checkoutPaymentService;
    private final TicketPricingService ticketPricingService;
    private final SeatReservationRepository seatReservationRepository;
    private final ShowtimeRepository showtimeRepository;
    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CheckoutOrderService(CheckoutPaymentService checkoutPaymentService,
                                TicketPricingService ticketPricingService,
                                SeatReservationRepository seatReservationRepository,
                                ShowtimeRepository showtimeRepository,
                                OrderRepository orderRepository,
                                TicketRepository ticketRepository,
                                PaymentTransactionRepository paymentTransactionRepository,
                                UserRepository userRepository,
                                ApplicationEventPublisher eventPublisher) {
        this.checkoutPaymentService = checkoutPaymentService;
        this.ticketPricingService = ticketPricingService;
        this.seatReservationRepository = seatReservationRepository;
        this.showtimeRepository = showtimeRepository;
        this.orderRepository = orderRepository;
        this.ticketRepository = ticketRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public OrderHistoryResponse placeOrder(Long userId,
                                           String sessionId,
                                           CheckoutPaymentRequest request) {
        PaymentCardSnapshot card = checkoutPaymentService.requireSavedCard(
                userId, request.paymentCardId());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoPendingBookingException());

        List<SeatReservation> held = seatReservationRepository
                .findHeldBySessionIdForUpdate(sessionId, SeatReservation.STATUS_HELD);
        if (held.isEmpty()) {
            throw new NoPendingBookingException();
        }

        Long showtimeId = held.get(0).getShowtimeId();
        if (held.stream().anyMatch(reservation -> !showtimeId.equals(reservation.getShowtimeId()))) {
            throw new NoPendingBookingException();
        }
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(NoPendingBookingException::new);

        Instant placedAt = Instant.now();
        BigDecimal subtotal = held.stream()
                .map(reservation -> ticketPricingService.priceFor(reservation.getTicketType()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal tax = subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(tax);

        Order order = new Order();
        order.setConfirmationNumber(newConfirmationNumber(placedAt));
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
        order = orderRepository.saveAndFlush(order);

        List<Ticket> tickets = new ArrayList<>();
        for (SeatReservation reservation : held) {
            Ticket ticket = new Ticket();
            ticket.setOrderId(order.getId());
            ticket.setShowtimeId(showtimeId);
            ticket.setSeatLabel(reservation.getSeatLabel());
            ticket.setTicketType(reservation.getTicketType());
            ticket.setUnitPrice(ticketPricingService.priceFor(reservation.getTicketType()));
            tickets.add(ticket);
        }
        try {
            tickets = ticketRepository.saveAllAndFlush(tickets);
        } catch (DataIntegrityViolationException ex) {
            throw new SeatUnavailableException(
                    tickets.stream().map(Ticket::getSeatLabel).toList());
        }

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setOrderId(order.getId());
        transaction.setStatus(PaymentTransaction.STATUS_APPROVED);
        transaction.setAmount(total);
        transaction.setTransactionReference("DEMO-" + UUID.randomUUID());
        transaction.setCardBrand(card.cardBrand());
        transaction.setCardLastFour(card.lastFour());
        transaction.setProcessedAt(placedAt);
        paymentTransactionRepository.save(transaction);

        // Once tickets exist, they are the permanent sold-seat records.
        seatReservationRepository.deleteAll(held);

        List<String> seats = tickets.stream()
                .map(Ticket::getSeatLabel)
                .sorted(BookingService::compareSeatLabels)
                .toList();
        List<TicketLine> ticketLines = summarizeTickets(tickets);

        eventPublisher.publishEvent(new OrderConfirmationEvent(
                order.getConfirmationNumber(),
                order.getConfirmationEmail(),
                user.getFirstName(),
                showtime.getMovie().getTitle(),
                showtime.getShowDate(),
                showtime.getShowTime(),
                showtime.getShowroom().getName(),
                seats,
                ticketLines,
                subtotal,
                tax,
                total,
                card.cardBrand(),
                card.lastFour()
        ));

        return new OrderHistoryResponse(
                order.getConfirmationNumber(),
                order.getStatus(),
                showtime.getMovie().getTitle(),
                showtime.getShowDate(),
                showtime.getShowTime(),
                showtime.getShowroom().getName(),
                tickets.stream()
                        .sorted((left, right) -> BookingService.compareSeatLabels(
                                left.getSeatLabel(), right.getSeatLabel()))
                        .map(ticket -> new OrderTicketResponse(
                                ticket.getSeatLabel(), ticket.getTicketType(), ticket.getUnitPrice()))
                        .toList(),
                subtotal,
                tax,
                total,
                card.cardBrand(),
                card.lastFour(),
                placedAt
        );
    }

    private List<TicketLine> summarizeTickets(List<Ticket> tickets) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        List.of("ADULT", "SENIOR", "CHILD").forEach(type -> counts.put(type, 0));
        tickets.forEach(ticket -> counts.merge(ticket.getTicketType(), 1, Integer::sum));

        return counts.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .map(entry -> {
                    double price = ticketPricingService.priceFor(entry.getKey()).doubleValue();
                    return new TicketLine(
                            entry.getKey(), entry.getValue(), price, price * entry.getValue());
                })
                .toList();
    }

    private String newConfirmationNumber(Instant placedAt) {
        String date = LocalDate.ofInstant(placedAt, ZoneOffset.UTC)
                .format(DateTimeFormatter.BASIC_ISO_DATE);
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "CES-" + date + "-" + suffix;
    }
}
