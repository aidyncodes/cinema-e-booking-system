package edu.uga.ces.service;

import edu.uga.ces.dto.CheckoutPaymentRequest;
import edu.uga.ces.dto.OrderConfirmationResponse;
import edu.uga.ces.dto.PaymentCardSnapshot;
import edu.uga.ces.dto.TicketLine;
import edu.uga.ces.event.OrderConfirmationEvent;
import edu.uga.ces.exception.AuthenticationRequiredException;
import edu.uga.ces.exception.NoPendingBookingException;
import edu.uga.ces.exception.ShowtimeNotFoundException;
import edu.uga.ces.model.SeatReservation;
import edu.uga.ces.model.Showtime;
import edu.uga.ces.model.User;
import edu.uga.ces.repository.SeatReservationRepository;
import edu.uga.ces.repository.ShowtimeRepository;
import edu.uga.ces.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finalizes checkout for the seats this session currently holds:
 *   1. Resolves and validates the buyer's saved card (CheckoutPaymentService).
 *   2. Moves the held SeatReservation rows to BOOKED and attaches the buyer.
 *   3. Publishes an OrderConfirmationEvent so OrderConfirmationEmailService
 *      sends the receipt only after this transaction commits.
 */
@Service
public class OrderFinalizationService {

    // Flat sales-tax rate. 
    // table, so this single constant stands in for one.
    private static final BigDecimal TAX_RATE = new BigDecimal("0.08");

    // Same ticket prices BookingService uses for the pre-checkout summary.
    // Kept in sync manually since there's no shared pricing table yet.
    private static final Map<String, Double> PRICES = Map.of(
            "ADULT", 12.00,
            "SENIOR", 8.00,
            "CHILD", 6.00
    );
    private static final List<String> TICKET_TYPES = List.of("ADULT", "SENIOR", "CHILD");

    // Avoids ambiguous characters in the printed confirmation code.
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final Pattern SEAT_LABEL = Pattern.compile("^([A-Z])(\\d+)$");

    private final SeatReservationRepository seatReservationRepository;
    private final ShowtimeRepository showtimeRepository;
    private final UserRepository userRepository;
    private final CheckoutPaymentService checkoutPaymentService;
    private final ApplicationEventPublisher eventPublisher;

    public OrderFinalizationService(SeatReservationRepository seatReservationRepository,
                                    ShowtimeRepository showtimeRepository,
                                    UserRepository userRepository,
                                    CheckoutPaymentService checkoutPaymentService,
                                    ApplicationEventPublisher eventPublisher) {
        this.seatReservationRepository = seatReservationRepository;
        this.showtimeRepository = showtimeRepository;
        this.userRepository = userRepository;
        this.checkoutPaymentService = checkoutPaymentService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public OrderConfirmationResponse confirmOrder(String sessionId, Long userId, CheckoutPaymentRequest request) {
        List<SeatReservation> held = seatReservationRepository
                .findBySessionIdAndStatus(sessionId, SeatReservation.STATUS_HELD);
        if (held.isEmpty()) {
            throw new NoPendingBookingException();
        }

        // All held seats belong to the same showtime (enforced when holding).
        Long showtimeId = held.get(0).getShowtimeId();
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ShowtimeNotFoundException(showtimeId));

        User buyer = userRepository.findById(userId)
                .orElseThrow(AuthenticationRequiredException::new);

        // Validates the card belongs to this user and hasn't expired; throws
        // AccountOperationException otherwise.
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
            double price = PRICES.getOrDefault(type, 0.0);
            double lineTotal = price * count;
            subtotal = subtotal.add(BigDecimal.valueOf(lineTotal));
            lines.add(new TicketLine(type, count, price, lineTotal));
        }
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal tax = subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(tax);

        String confirmationNumber = generateConfirmationNumber();

        // Finalize: this is the moment a hold becomes a real, paid booking.
        for (SeatReservation reservation : held) {
            reservation.setStatus(SeatReservation.STATUS_BOOKED);
            reservation.setUserId(userId);
        }
        seatReservationRepository.saveAll(held);

        // Listener runs after this transaction commits, so an email can never
        // go out for a purchase that failed to save.
        eventPublisher.publishEvent(new OrderConfirmationEvent(
                confirmationNumber,
                request.confirmationEmail(),
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

    // Short, printable confirmation code, ex "CES-8F3K2Q".
    private String generateConfirmationNumber() {
        StringBuilder code = new StringBuilder("CES-");
        for (int i = 0; i < 6; i++) {
            code.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        return code.toString();
    }

    // Same seat ordering BookingService uses: row letter, then seat number
    // (so A2 sorts before A10, not after it).
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