package edu.uga.ces.service;

import edu.uga.ces.dto.AddressRequest;
import edu.uga.ces.dto.PaymentCardRequest;
import edu.uga.ces.dto.PaymentCardResponse;
import edu.uga.ces.dto.ProfileResponse;
import edu.uga.ces.dto.ProfileUpdateRequest;
import edu.uga.ces.exception.AccountOperationException;
import edu.uga.ces.model.Address;
import edu.uga.ces.model.PaymentCard;
import edu.uga.ces.model.User;
import edu.uga.ces.repository.AddressRepository;
import edu.uga.ces.repository.PaymentCardRepository;
import edu.uga.ces.repository.UserRepository;
import edu.uga.ces.repository.UserLockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfileServiceTests {

    private static final String TEST_KEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

    private UserRepository userRepository;
    private UserLockRepository userLockRepository;
    private AddressRepository addressRepository;
    private PaymentCardRepository paymentCardRepository;
    private AccountEmailService emailService;
    private ProfileService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userLockRepository = mock(UserLockRepository.class);
        addressRepository = mock(AddressRepository.class);
        paymentCardRepository = mock(PaymentCardRepository.class);
        emailService = mock(AccountEmailService.class);
        service = new ProfileService(
                userRepository,
                userLockRepository,
                addressRepository,
                paymentCardRepository,
                new CardEncryptionService(TEST_KEY),
                emailService);
    }

    @Test
    void profileResponseIncludesReadOnlyEmailAndMaskedCards() {
        User user = user(2L);
        PaymentCard card = new PaymentCard();
        card.setId(11L);
        card.setUserId(2L);
        card.setCardholderName("Demo Customer");
        card.setCardBrand("Visa");
        card.setLastFour("1111");
        card.setExpirationMonth(12);
        card.setExpirationYear(2030);
        card.setEncryptedCardData("ciphertext".getBytes(StandardCharsets.UTF_8));
        card.setEncryptionIv(new byte[12]);

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(addressRepository.findByUserId(2L)).thenReturn(Optional.empty());
        when(paymentCardRepository.findByUserIdOrderByIdAsc(2L)).thenReturn(List.of(card));

        ProfileResponse response = service.getProfile(2L);

        assertEquals("customer@example.com", response.email());
        assertEquals("1111", response.paymentCards().get(0).lastFour());
    }

    @Test
    void rejectsFourthPaymentCard() {
        when(userLockRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(user(2L)));
        when(paymentCardRepository.countByUserId(2L)).thenReturn(3L);

        assertThrows(AccountOperationException.class,
                () -> service.addPaymentCard(2L, validCardRequest()));
        verify(paymentCardRepository, never()).save(any());
    }

    @Test
    void addedCardIsEncryptedAndResponseOnlyReturnsLastFour() {
        User user = user(2L);
        when(userLockRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(user));
        when(paymentCardRepository.countByUserId(2L)).thenReturn(2L);
        when(paymentCardRepository.save(any(PaymentCard.class))).thenAnswer(invocation -> {
            PaymentCard card = invocation.getArgument(0);
            card.setId(12L);
            return card;
        });

        PaymentCardResponse response = service.addPaymentCard(2L, validCardRequest());

        assertEquals("1111", response.lastFour());
        assertEquals("Visa", response.cardBrand());
        verify(paymentCardRepository).save(any(PaymentCard.class));
        verify(emailService).sendProfileChangedEmail(user.getEmail(), user.getFirstName());
    }

    @Test
    void updateUpsertsSingleAddressAndKeepsEmailUnchanged() {
        User user = user(2L);
        Address existing = new Address();
        existing.setId(4L);
        existing.setUserId(2L);
        existing.setStreetLine1("Old Street");
        existing.setCity("Athens");
        existing.setState("GA");
        existing.setPostalCode("30602");
        existing.setCountry("USA");

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(addressRepository.findByUserId(2L)).thenReturn(Optional.of(existing));
        when(paymentCardRepository.findByUserIdOrderByIdAsc(2L)).thenReturn(List.of());

        ProfileUpdateRequest request = new ProfileUpdateRequest(
                "Updated", "Customer", "706-555-0199", true,
                new AddressRequest("New Street", null, "Athens", "GA", "30605", "USA"));

        ProfileResponse response = service.updateProfile(2L, request);

        assertEquals("customer@example.com", response.email());
        assertEquals(4L, response.address().id());
        assertEquals("New Street", response.address().streetLine1());
        verify(addressRepository).save(existing);
        verify(emailService).sendProfileChangedEmail(user.getEmail(), user.getFirstName());
    }

    private PaymentCardRequest validCardRequest() {
        return new PaymentCardRequest("Demo Customer", "4111-1111-1111-1111", 12, 2030);
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("customer@example.com");
        user.setFirstName("Demo");
        user.setLastName("Customer");
        user.setPhone("706-555-0101");
        user.setPromotionsOptIn(false);
        user.setPasswordHash("hash");
        return user;
    }
}
