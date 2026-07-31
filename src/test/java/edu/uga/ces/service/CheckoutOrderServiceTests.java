package edu.uga.ces.service;

import edu.uga.ces.dto.CheckoutPaymentRequest;
import edu.uga.ces.dto.OrderHistoryResponse;
import edu.uga.ces.dto.PaymentCardSnapshot;
import edu.uga.ces.event.OrderConfirmationEvent;
import edu.uga.ces.exception.NoPendingBookingException;
import edu.uga.ces.model.Movie;
import edu.uga.ces.model.Order;
import edu.uga.ces.model.SeatReservation;
import edu.uga.ces.model.Showroom;
import edu.uga.ces.model.Showtime;
import edu.uga.ces.repository.OrderRepository;
import edu.uga.ces.repository.PaymentTransactionRepository;
import edu.uga.ces.repository.SeatReservationRepository;
import edu.uga.ces.repository.ShowtimeRepository;
import edu.uga.ces.repository.TicketRepository;
import edu.uga.ces.repository.UserRepository;
import edu.uga.ces.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CheckoutOrderServiceTests {

    private CheckoutPaymentService paymentService;
    private TicketPricingService ticketPricingService;
    private SeatReservationRepository reservationRepository;
    private ShowtimeRepository showtimeRepository;
    private OrderRepository orderRepository;
    private TicketRepository ticketRepository;
    private PaymentTransactionRepository transactionRepository;
    private UserRepository userRepository;
    private ApplicationEventPublisher eventPublisher;
    private CheckoutOrderService service;

    @BeforeEach
    void setUp() {
        paymentService = mock(CheckoutPaymentService.class);
        ticketPricingService = mock(TicketPricingService.class);
        reservationRepository = mock(SeatReservationRepository.class);
        showtimeRepository = mock(ShowtimeRepository.class);
        orderRepository = mock(OrderRepository.class);
        ticketRepository = mock(TicketRepository.class);
        transactionRepository = mock(PaymentTransactionRepository.class);
        userRepository = mock(UserRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new CheckoutOrderService(
                paymentService,
                ticketPricingService,
                reservationRepository,
                showtimeRepository,
                orderRepository,
                ticketRepository,
                transactionRepository,
                userRepository,
                eventPublisher);
        when(ticketPricingService.priceFor("ADULT")).thenReturn(new BigDecimal("12.00"));
        when(ticketPricingService.priceFor("SENIOR")).thenReturn(new BigDecimal("8.00"));
        when(ticketPricingService.priceFor("CHILD")).thenReturn(new BigDecimal("6.00"));
    }

    @Test
    void convertsHeldSeatsIntoPaidOrder() {
        CheckoutPaymentRequest request =
                new CheckoutPaymentRequest(7L, "receipt@example.com");
        when(paymentService.requireSavedCard(2L, 7L))
                .thenReturn(new PaymentCardSnapshot(7L, "Visa", "1111"));

        User user = new User();
        user.setId(2L);
        user.setFirstName("Test");
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        List<SeatReservation> held = List.of(
                reservation(1L, "A1", "ADULT"),
                reservation(1L, "A2", "CHILD"));
        when(reservationRepository.findHeldBySessionIdForUpdate(
                "session-1", SeatReservation.STATUS_HELD)).thenReturn(held);
        when(showtimeRepository.findById(1L)).thenReturn(Optional.of(showtime()));
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(10L);
            return order;
        });
        when(ticketRepository.saveAllAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrderHistoryResponse result = service.placeOrder(2L, "session-1", request);

        assertEquals(Order.STATUS_PAID, result.status());
        assertEquals(new BigDecimal("18.00"), result.subtotal());
        assertEquals(new BigDecimal("1.44"), result.taxAmount());
        assertEquals(new BigDecimal("19.44"), result.totalAmount());
        assertEquals(List.of("A1", "A2"),
                result.tickets().stream().map(ticket -> ticket.seatLabel()).toList());
        verify(reservationRepository).deleteAll(held);
        verify(transactionRepository).save(any());
        verify(eventPublisher).publishEvent(any(OrderConfirmationEvent.class));
    }

    @Test
    void refusesPaymentWhenSessionHasNoHeldSeats() {
        CheckoutPaymentRequest request =
                new CheckoutPaymentRequest(7L, "receipt@example.com");
        when(paymentService.requireSavedCard(2L, 7L))
                .thenReturn(new PaymentCardSnapshot(7L, "Visa", "1111"));
        User user = new User();
        user.setId(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(reservationRepository.findHeldBySessionIdForUpdate(
                "session-1", SeatReservation.STATUS_HELD)).thenReturn(List.of());

        assertThrows(NoPendingBookingException.class,
                () -> service.placeOrder(2L, "session-1", request));
    }

    private SeatReservation reservation(Long showtimeId, String seat, String type) {
        SeatReservation reservation = new SeatReservation();
        reservation.setShowtimeId(showtimeId);
        reservation.setSeatLabel(seat);
        reservation.setTicketType(type);
        reservation.setStatus(SeatReservation.STATUS_HELD);
        reservation.setSessionId("session-1");
        return reservation;
    }

    private Showtime showtime() {
        Movie movie = new Movie();
        movie.setId(3L);
        movie.setTitle("Test Movie");

        Showroom showroom = new Showroom();
        showroom.setId(4L);
        showroom.setName("Showroom 4");

        Showtime showtime = new Showtime();
        showtime.setId(1L);
        showtime.setMovie(movie);
        showtime.setShowroom(showroom);
        showtime.setShowDate(LocalDate.of(2026, 8, 1));
        showtime.setShowTime(LocalTime.of(19, 30));
        return showtime;
    }
}
