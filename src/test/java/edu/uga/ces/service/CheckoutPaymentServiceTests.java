package edu.uga.ces.service;

import edu.uga.ces.dto.PaymentCardSnapshot;
import edu.uga.ces.exception.AccountOperationException;
import edu.uga.ces.model.PaymentCard;
import edu.uga.ces.repository.PaymentCardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CheckoutPaymentServiceTests {

    private static final String TEST_KEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

    private PaymentCardRepository paymentCardRepository;
    private CheckoutPaymentService service;

    @BeforeEach
    void setUp() {
        paymentCardRepository = mock(PaymentCardRepository.class);
        service = new CheckoutPaymentService(
                paymentCardRepository,
                new CardEncryptionService(TEST_KEY));
    }

    @Test
    void returnsSafeSnapshotForCardOwnedByAuthenticatedUser() {
        PaymentCard card = validCard(7L, 2L);
        when(paymentCardRepository.findByIdAndUserId(7L, 2L))
                .thenReturn(Optional.of(card));

        PaymentCardSnapshot snapshot = service.requireSavedCard(2L, 7L);

        assertEquals(7L, snapshot.paymentCardId());
        assertEquals("Visa", snapshot.cardBrand());
        assertEquals("1111", snapshot.lastFour());
        verify(paymentCardRepository).findByIdAndUserId(7L, 2L);
    }

    @Test
    void rejectsCardThatDoesNotBelongToAuthenticatedUser() {
        when(paymentCardRepository.findByIdAndUserId(7L, 2L))
                .thenReturn(Optional.empty());

        AccountOperationException exception = assertThrows(
                AccountOperationException.class,
                () -> service.requireSavedCard(2L, 7L));

        assertEquals(
                "The selected payment card was not found on your account.",
                exception.getMessage());
    }

    @Test
    void rejectsExpiredSavedCard() {
        PaymentCard card = validCard(7L, 2L);
        YearMonth expired = YearMonth.now().minusMonths(1);
        card.setExpirationMonth(expired.getMonthValue());
        card.setExpirationYear(expired.getYear());
        when(paymentCardRepository.findByIdAndUserId(7L, 2L))
                .thenReturn(Optional.of(card));

        AccountOperationException exception = assertThrows(
                AccountOperationException.class,
                () -> service.requireSavedCard(2L, 7L));

        assertEquals("The payment card has expired.", exception.getMessage());
    }

    @Test
    void rejectsCardWithInvalidOrderSnapshotValues() {
        PaymentCard card = validCard(7L, 2L);
        card.setLastFour("11");
        when(paymentCardRepository.findByIdAndUserId(7L, 2L))
                .thenReturn(Optional.of(card));

        AccountOperationException exception = assertThrows(
                AccountOperationException.class,
                () -> service.requireSavedCard(2L, 7L));

        assertEquals(
                "The selected payment card cannot be used. Update it in your profile.",
                exception.getMessage());
    }

    private PaymentCard validCard(Long id, Long userId) {
        YearMonth future = YearMonth.now().plusYears(2);
        PaymentCard card = new PaymentCard();
        card.setId(id);
        card.setUserId(userId);
        card.setCardBrand("Visa");
        card.setLastFour("1111");
        card.setExpirationMonth(future.getMonthValue());
        card.setExpirationYear(future.getYear());
        return card;
    }
}
