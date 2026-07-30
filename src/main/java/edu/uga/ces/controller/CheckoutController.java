package edu.uga.ces.controller;

import edu.uga.ces.dto.CheckoutPaymentRequest;
import edu.uga.ces.dto.OrderHistoryResponse;
import edu.uga.ces.service.CheckoutOrderService;
import edu.uga.ces.service.SessionUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    private final CheckoutOrderService checkoutOrderService;
    private final SessionUserService sessionUserService;

    public CheckoutController(CheckoutOrderService checkoutOrderService,
                              SessionUserService sessionUserService) {
        this.checkoutOrderService = checkoutOrderService;
        this.sessionUserService = sessionUserService;
    }

    @PostMapping("/payment")
    public ResponseEntity<OrderHistoryResponse> pay(
            @Valid @RequestBody CheckoutPaymentRequest body,
            HttpServletRequest request) {
        Long userId = sessionUserService.requireUserId(request);
        HttpSession session = request.getSession(false);
        OrderHistoryResponse order = checkoutOrderService.placeOrder(
                userId, session.getId(), body);
        session.removeAttribute(BookingController.CHECKOUT_EMAIL_ATTRIBUTE);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }
}
