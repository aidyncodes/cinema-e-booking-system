package edu.uga.ces.service;

import edu.uga.ces.dto.OrderHistoryResponse;
import edu.uga.ces.dto.OrderTicketResponse;
import edu.uga.ces.model.Order;
import edu.uga.ces.model.Showtime;
import edu.uga.ces.model.Ticket;
import edu.uga.ces.exception.OrderNotFoundException;
import edu.uga.ces.repository.OrderRepository;
import edu.uga.ces.repository.ShowtimeRepository;
import edu.uga.ces.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Read side of the order flow: returns a logged-in user's past orders with their
 * tickets and show details, for the order-history view. The write side (creating
 * orders) lives in the checkout finalization flow.
 */
@Service
public class OrderHistoryService {

    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    private final ShowtimeRepository showtimeRepository;

    public OrderHistoryService(OrderRepository orderRepository,
                               TicketRepository ticketRepository,
                               ShowtimeRepository showtimeRepository) {
        this.orderRepository = orderRepository;
        this.ticketRepository = ticketRepository;
        this.showtimeRepository = showtimeRepository;
    }

    @Transactional(readOnly = true)
    public List<OrderHistoryResponse> getOrdersForUser(Long userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByPlacedAtDescIdDesc(userId);
        if (orders.isEmpty()) {
            return List.of();
        }

        // Load every order's tickets in one query, then group them by order id.
        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        Map<Long, List<Ticket>> ticketsByOrder = ticketRepository
                .findByOrderIdInOrderBySeatLabelAsc(orderIds).stream()
                .collect(Collectors.groupingBy(Ticket::getOrderId));

        return orders.stream()
                .map(order -> toResponse(order, ticketsByOrder.getOrDefault(order.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderHistoryResponse getOrderForUser(Long userId, String confirmationNumber) {
        Order order = orderRepository
                .findByConfirmationNumberAndUserId(confirmationNumber, userId)
                .orElseThrow(() -> new OrderNotFoundException(confirmationNumber));
        List<Ticket> tickets = ticketRepository.findByOrderIdOrderBySeatLabelAsc(order.getId());
        return toResponse(order, tickets);
    }

    private OrderHistoryResponse toResponse(Order order, List<Ticket> tickets) {
        // Look up the show details for display. The order keeps a showtime id;
        // if the showtime was somehow removed we still show the order gracefully.
        Showtime showtime = showtimeRepository.findById(order.getShowtimeId()).orElse(null);
        String movieTitle = showtime != null ? showtime.getMovie().getTitle() : "Unknown movie";
        LocalDate showDate = showtime != null ? showtime.getShowDate() : null;
        LocalTime showTime = showtime != null ? showtime.getShowTime() : null;
        String showroomName = showtime != null ? showtime.getShowroom().getName() : "Unknown showroom";

        List<OrderTicketResponse> ticketLines = tickets.stream()
                .map(ticket -> new OrderTicketResponse(
                        ticket.getSeatLabel(), ticket.getTicketType(), ticket.getUnitPrice()))
                .toList();

        return new OrderHistoryResponse(
                order.getConfirmationNumber(),
                order.getStatus(),
                movieTitle,
                showDate,
                showTime,
                showroomName,
                ticketLines,
                order.getSubtotal(),
                order.getTaxAmount(),
                order.getTotalAmount(),
                order.getPaymentCardBrand(),
                order.getPaymentCardLastFour(),
                order.getPlacedAt()
        );
    }
}
