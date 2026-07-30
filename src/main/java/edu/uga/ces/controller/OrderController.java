package edu.uga.ces.controller;

import edu.uga.ces.service.OrderHistoryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Order history for the logged-in user.
 *   GET /api/profile/orders - list my past orders, newest first
 *
 * Scoped to the session user (set at login in AuthController), mirroring the
 * favorites endpoints: no session -> 401, regardless of SecurityConfig.
 */
@RestController
@RequestMapping("/api/profile/orders")
public class OrderController {

    private final OrderHistoryService orderHistoryService;

    public OrderController(OrderHistoryService orderHistoryService) {
        this.orderHistoryService = orderHistoryService;
    }

    @GetMapping
    public ResponseEntity<?> list(HttpServletRequest request) {
        Long userId = currentUserId(request);
        if (userId == null) {
            return notAuthenticated();
        }
        return ResponseEntity.ok(orderHistoryService.getOrdersForUser(userId));
    }

    // Reads the logged-in user's id from the session; null when nobody is logged in.
    private Long currentUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object userId = session.getAttribute("userId");
        return (userId instanceof Number number) ? number.longValue() : null;
    }

    private ResponseEntity<Map<String, String>> notAuthenticated() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "NOT_AUTHENTICATED",
                        "message", "You must be logged in to view your orders."));
    }
}
