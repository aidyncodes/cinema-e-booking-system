package edu.uga.ces.controller;

import edu.uga.ces.dto.CheckoutEmailRequest;
import edu.uga.ces.dto.HoldSeatsRequest;
import edu.uga.ces.dto.OrderSummaryResponse;
import edu.uga.ces.dto.SeatMapResponse;
import edu.uga.ces.service.BookingService;
import edu.uga.ces.service.SessionUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Booking endpoints for the seat-selection and checkout flow.
 *   GET  /api/showtimes/{id}/seats  - seat map (layout + taken seats) for a showtime
 *   POST /api/showtimes/{id}/hold   - hold the selected seats for this session
 *   GET  /api/checkout/summary      - order summary for the seats this session holds
 *
 * Seats are held against the HttpSession id so a guest can select seats before
 * logging in. Login rotates that id and transfers the hold to the new id.
 * userId is attached when the visitor is already authenticated.
 */
@RestController
@RequestMapping("/api")
public class BookingController {

    public static final String CHECKOUT_EMAIL_ATTRIBUTE = "checkoutConfirmationEmail";

    private final BookingService bookingService;
    private final SessionUserService sessionUserService;

    public BookingController(BookingService bookingService,
                             SessionUserService sessionUserService) {
        this.bookingService = bookingService;
        this.sessionUserService = sessionUserService;
    }

    @GetMapping("/showtimes/{id}/seats")
    public SeatMapResponse getSeatMap(@PathVariable Long id, HttpServletRequest request) {
        return bookingService.getSeatMap(id, sessionId(request));
    }

    @PostMapping("/showtimes/{id}/hold")
    public SeatMapResponse holdSeats(@PathVariable Long id,
                                     @Valid @RequestBody HoldSeatsRequest body,
                                     HttpServletRequest request) {
        return bookingService.holdSeats(id, sessionId(request), currentUserId(request), body);
    }

    @GetMapping("/checkout/summary")
    public OrderSummaryResponse getOrderSummary(HttpServletRequest request) {
        return bookingService.getOrderSummary(sessionId(request));
    }

    @PostMapping("/checkout/confirmation-email")
    public Map<String, String> saveConfirmationEmail(
            @Valid @RequestBody CheckoutEmailRequest body,
            HttpServletRequest request) {
        sessionUserService.requireUserId(request);
        String email = body.confirmationEmail().trim();
        request.getSession(true).setAttribute(CHECKOUT_EMAIL_ATTRIBUTE, email);
        return Map.of("confirmationEmail", email);
    }

    @GetMapping("/checkout/confirmation-email")
    public Map<String, String> getConfirmationEmail(HttpServletRequest request) {
        sessionUserService.requireUserId(request);
        HttpSession session = request.getSession(false);
        Object email = session.getAttribute(CHECKOUT_EMAIL_ATTRIBUTE);
        return Map.of("confirmationEmail", email instanceof String ? (String) email : "");
    }

    // Guests need a stable session id to hold seats, so create one if absent.
    private String sessionId(HttpServletRequest request) {
        return request.getSession(true).getId();
    }

    // The logged-in user's id from the session, or null for a guest.
    private Long currentUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object userId = session.getAttribute("userId");
        return (userId instanceof Number number) ? number.longValue() : null;
    }
}
