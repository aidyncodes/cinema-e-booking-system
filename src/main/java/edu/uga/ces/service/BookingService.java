package edu.uga.ces.service;

import edu.uga.ces.dto.HoldSeatsRequest;
import edu.uga.ces.dto.OrderSummaryResponse;
import edu.uga.ces.dto.SeatMapResponse;
import edu.uga.ces.dto.SeatStatus;
import edu.uga.ces.dto.ShowroomResponse;
import edu.uga.ces.dto.TicketLine;
import edu.uga.ces.exception.NoPendingBookingException;
import edu.uga.ces.exception.SeatSelectionException;
import edu.uga.ces.exception.SeatUnavailableException;
import edu.uga.ces.exception.ShowtimeNotFoundException;
import edu.uga.ces.model.SeatReservation;
import edu.uga.ces.model.Showroom;
import edu.uga.ces.model.Showtime;
import edu.uga.ces.model.Ticket;
import edu.uga.ces.repository.SeatReservationRepository;
import edu.uga.ces.repository.ShowtimeRepository;
import edu.uga.ces.repository.TicketRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Booking flow: build the seat map for a showtime, hold seats against the
 * current session, and produce the checkout order summary. Ticket prices are
 * constants for this sprint; taxes, fees, and promotions are out of scope.
 */
@Service
public class BookingService {

    private static final double TAX_RATE = 0.08;
    // Age-category ticket prices. A reference table can replace this later.
    private static final Map<String, Double> PRICES = Map.of(
            "ADULT", 12.00,
            "SENIOR", 8.00,
            "CHILD", 6.00
    );

    // Ticket types listed in the fixed order shown on the summary.
    private static final List<String> TICKET_TYPES = List.of("ADULT", "SENIOR", "CHILD");

    // How long a held seat stays reserved before SeatHoldCleanupJob can release
    // it back to everyone else. 10 minutes is generous for filling out checkout.
    static final Duration HOLD_DURATION = Duration.ofMinutes(10);

    private static final Pattern SEAT_LABEL = Pattern.compile("^([A-Z])(\\d+)$");

    private final ShowtimeRepository showtimeRepository;
    private final SeatReservationRepository seatReservationRepository;
    private final TicketRepository ticketRepository;

    public BookingService(ShowtimeRepository showtimeRepository,
                          SeatReservationRepository seatReservationRepository,
                          TicketRepository ticketRepository) {
        this.showtimeRepository = showtimeRepository;
        this.seatReservationRepository = seatReservationRepository;
        this.ticketRepository = ticketRepository;
    }

    @Transactional(readOnly = true)
    public SeatMapResponse getSeatMap(Long showtimeId, String sessionId) {
        Showtime showtime = loadShowtime(showtimeId);
        return buildSeatMap(showtime, sessionId);
    }

    @Transactional
    public SeatMapResponse holdSeats(Long showtimeId, String sessionId, Long userId, HoldSeatsRequest request) {
        Showtime showtime = loadShowtime(showtimeId);
        Showroom showroom = showtime.getShowroom();

        List<String> seats = normalizeSeats(request.seats());
        validateSeatLabels(seats, showroom);

        int totalTickets = request.adultCount() + request.seniorCount() + request.childCount();
        if (totalTickets == 0) {
            throw new SeatSelectionException("Choose at least one ticket before selecting seats.");
        }
        if (seats.size() != totalTickets) {
            throw new SeatSelectionException(
                    "Select exactly " + totalTickets + " seat" + (totalTickets == 1 ? "" : "s")
                            + " to match your ticket count.");
        }

        // Reject seats already taken by anyone other than this session.
        List<String> taken = seatReservationRepository.findByShowtimeId(showtimeId).stream()
                .filter(reservation -> seats.contains(reservation.getSeatLabel()))
                .filter(reservation -> !sessionId.equals(reservation.getSessionId()))
                .map(SeatReservation::getSeatLabel)
                .sorted()
                .toList();
        if (!taken.isEmpty()) {
            throw new SeatUnavailableException(taken);
        }
        List<String> sold = ticketRepository.findByShowtimeId(showtimeId).stream()
                .map(Ticket::getSeatLabel)
                .filter(seats::contains)
                .sorted(BookingService::compareSeatLabels)
                .toList();
        if (!sold.isEmpty()) {
            throw new SeatUnavailableException(sold);
        }

        // A session holds one showtime at a time, so drop any earlier holds first.
        seatReservationRepository.deleteBySessionIdAndStatus(sessionId, SeatReservation.STATUS_HELD);

        List<String> ticketTypePerSeat = assignTicketTypes(
                request.adultCount(), request.seniorCount(), request.childCount());

        Instant expiresAt = Instant.now().plus(HOLD_DURATION);
        List<SeatReservation> holds = new ArrayList<>();
        for (int index = 0; index < seats.size(); index++) {
            SeatReservation reservation = new SeatReservation();
            reservation.setShowtimeId(showtimeId);
            reservation.setSeatLabel(seats.get(index));
            reservation.setStatus(SeatReservation.STATUS_HELD);
            reservation.setTicketType(ticketTypePerSeat.get(index));
            reservation.setSessionId(sessionId);
            reservation.setUserId(userId);
            reservation.setExpiresAt(expiresAt);
            holds.add(reservation);
        }

        try {
            // Flush now so the unique (showtime, seat) constraint runs before we
            // return, closing the race between the check above and the insert.
            seatReservationRepository.saveAll(holds);
            seatReservationRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new SeatUnavailableException(seats);
        }

        return buildSeatMap(showtime, sessionId);
    }

    @Transactional(readOnly = true)
    public OrderSummaryResponse getOrderSummary(String sessionId) {
        List<SeatReservation> held = seatReservationRepository
                .findBySessionIdAndStatus(sessionId, SeatReservation.STATUS_HELD);
        if (held.isEmpty()) {
            throw new NoPendingBookingException();
        }

        // All held seats belong to the same showtime (enforced when holding).
        Showtime showtime = loadShowtime(held.get(0).getShowtimeId());

        List<String> seats = held.stream()
                .map(SeatReservation::getSeatLabel)
                .sorted(BookingService::compareSeatLabels)
                .toList();

        // Count tickets per type from the seats actually held.
        Map<String, Integer> counts = new LinkedHashMap<>();
        TICKET_TYPES.forEach(type -> counts.put(type, 0));
        for (SeatReservation reservation : held) {
            counts.merge(reservation.getTicketType(), 1, Integer::sum);
        }

        List<TicketLine> lines = new ArrayList<>();
        double total = 0.0;
        for (String type : TICKET_TYPES) {
            int count = counts.getOrDefault(type, 0);
            if (count == 0) {
                continue;
            }
            double price = PRICES.getOrDefault(type, 0.0);
            double lineTotal = price * count;
            total += lineTotal;
            lines.add(new TicketLine(type, count, price, lineTotal));
        }

        double taxAmount = Math.round(total * TAX_RATE * 100.0) / 100.0;
        return new OrderSummaryResponse(
                showtime.getId(),
                showtime.getMovie().getTitle(),
                showtime.getShowDate(),
                showtime.getShowTime(),
                showtime.getShowroom().getName(),
                seats,
                lines,
                held.size(),
                total,
                taxAmount,
                total + taxAmount
        );
    }

    private Showtime loadShowtime(Long showtimeId) {
        return showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ShowtimeNotFoundException(showtimeId));
    }

    private SeatMapResponse buildSeatMap(Showtime showtime, String sessionId) {
        List<SeatStatus> unavailable = new ArrayList<>(seatReservationRepository
                .findByShowtimeId(showtime.getId()).stream()
                .map(reservation -> new SeatStatus(
                        reservation.getSeatLabel(),
                        reservation.getStatus(),
                        SeatReservation.STATUS_HELD.equals(reservation.getStatus())
                                && sessionId.equals(reservation.getSessionId())))
                .toList());
        ticketRepository.findByShowtimeId(showtime.getId()).stream()
                .filter(ticket -> unavailable.stream()
                        .noneMatch(seat -> seat.seatLabel().equals(ticket.getSeatLabel())))
                .map(ticket -> new SeatStatus(
                        ticket.getSeatLabel(), SeatReservation.STATUS_BOOKED, false))
                .forEach(unavailable::add);

        Showroom showroom = showtime.getShowroom();
        ShowroomResponse showroomResponse = new ShowroomResponse(
                showroom.getId(),
                showroom.getName(),
                showroom.getRowCount(),
                showroom.getSeatsPerRow());

        return new SeatMapResponse(
                showtime.getId(),
                showtime.getMovie().getId(),
                showtime.getMovie().getTitle(),
                showtime.getShowDate(),
                showtime.getShowTime(),
                showroomResponse,
                unavailable);
    }

    // Uppercases, trims, and drops duplicates while keeping selection order.
    private List<String> normalizeSeats(List<String> seats) {
        List<String> normalized = new ArrayList<>();
        for (String seat : seats) {
            if (seat == null) {
                continue;
            }
            String label = seat.trim().toUpperCase();
            if (!label.isEmpty() && !normalized.contains(label)) {
                normalized.add(label);
            }
        }
        return normalized;
    }

    // Server-side check that every label is a real seat in this showroom.
    private void validateSeatLabels(List<String> seats, Showroom showroom) {
        int rows = showroom.getRowCount();
        int cols = showroom.getSeatsPerRow();
        for (String label : seats) {
            Matcher matcher = SEAT_LABEL.matcher(label);
            if (!matcher.matches()) {
                throw new SeatSelectionException("Invalid seat label: " + label);
            }
            int row = matcher.group(1).charAt(0) - 'A';
            int col = Integer.parseInt(matcher.group(2));
            if (row < 0 || row >= rows || col < 1 || col > cols) {
                throw new SeatSelectionException("Seat " + label + " is not in this showroom.");
            }
        }
    }

    // Builds a per-seat ticket-type list (adults first, then seniors, then children)
    // so each stored seat carries a valid age category.
    private List<String> assignTicketTypes(int adults, int seniors, int children) {
        List<String> types = new ArrayList<>();
        for (int i = 0; i < adults; i++) {
            types.add("ADULT");
        }
        for (int i = 0; i < seniors; i++) {
            types.add("SENIOR");
        }
        for (int i = 0; i < children; i++) {
            types.add("CHILD");
        }
        return types;
    }

    // Orders seat labels by row letter, then seat number (A2 before A10).
    static int compareSeatLabels(String left, String right) {
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

    /**
     * Login rotates the HTTP session id for security. Move any guest hold to the
     * rotated id so checkout can still find the selected seats.
     */
    @Transactional
    public void transferHeldSeats(String oldSessionId, String newSessionId, Long userId) {
        if (oldSessionId.equals(newSessionId)) {
            return;
        }
        List<SeatReservation> held = seatReservationRepository
                .findBySessionIdAndStatus(oldSessionId, SeatReservation.STATUS_HELD);
        held.forEach(reservation -> {
            reservation.setSessionId(newSessionId);
            reservation.setUserId(userId);
        });
        seatReservationRepository.saveAll(held);
    }
}