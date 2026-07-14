package edu.uga.ces.service;

import edu.uga.ces.dto.AddressRequest;
import edu.uga.ces.dto.AddressResponse;
import edu.uga.ces.dto.PaymentCardRequest;
import edu.uga.ces.dto.PaymentCardResponse;
import edu.uga.ces.dto.ProfileResponse;
import edu.uga.ces.dto.ProfileUpdateRequest;
import edu.uga.ces.exception.AccountOperationException;
import edu.uga.ces.exception.AuthenticationRequiredException;
import edu.uga.ces.model.Address;
import edu.uga.ces.model.PaymentCard;
import edu.uga.ces.model.User;
import edu.uga.ces.repository.AddressRepository;
import edu.uga.ces.repository.PaymentCardRepository;
import edu.uga.ces.repository.UserRepository;
import edu.uga.ces.repository.UserLockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class ProfileService {

    private static final long MAX_PAYMENT_CARDS = 3;

    private final UserRepository userRepository;
    private final UserLockRepository userLockRepository;
    private final AddressRepository addressRepository;
    private final PaymentCardRepository paymentCardRepository;
    private final CardEncryptionService cardEncryptionService;
    private final AccountEmailService emailService;

    public ProfileService(UserRepository userRepository,
                          UserLockRepository userLockRepository,
                          AddressRepository addressRepository,
                          PaymentCardRepository paymentCardRepository,
                          CardEncryptionService cardEncryptionService,
                          AccountEmailService emailService) {
        this.userRepository = userRepository;
        this.userLockRepository = userLockRepository;
        this.addressRepository = addressRepository;
        this.paymentCardRepository = paymentCardRepository;
        this.cardEncryptionService = cardEncryptionService;
        this.emailService = emailService;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long userId) {
        User user = requireUser(userId);
        return toResponse(user);
    }

    @Transactional
    public ProfileResponse updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = requireUser(userId);
        boolean changed = false;

        String firstName = request.firstName().trim();
        String lastName = request.lastName().trim();
        String phone = request.phone().trim();

        if (!Objects.equals(user.getFirstName(), firstName)) {
            user.setFirstName(firstName);
            changed = true;
        }
        if (!Objects.equals(user.getLastName(), lastName)) {
            user.setLastName(lastName);
            changed = true;
        }
        if (!Objects.equals(user.getPhone(), phone)) {
            user.setPhone(phone);
            changed = true;
        }
        if (!Objects.equals(user.getPromotionsOptIn(), request.promotionsOptIn())) {
            user.setPromotionsOptIn(request.promotionsOptIn());
            changed = true;
        }

        if (changed) {
            userRepository.save(user);
        }
        if (request.address() != null) {
            changed = upsertAddress(userId, request.address()) || changed;
        }

        if (changed) {
            emailService.sendProfileChangedEmail(user.getEmail(), user.getFirstName());
        }
        return toResponse(user);
    }

    @Transactional
    public void deleteAddress(Long userId) {
        User user = requireUser(userId);
        addressRepository.findByUserId(userId).ifPresent(address -> {
            addressRepository.delete(address);
            emailService.sendProfileChangedEmail(user.getEmail(), user.getFirstName());
        });
    }

    @Transactional
    public PaymentCardResponse addPaymentCard(Long userId, PaymentCardRequest request) {
        User user = requireLockedUser(userId);
        if (paymentCardRepository.countByUserId(userId) >= MAX_PAYMENT_CARDS) {
            throw new AccountOperationException("An account can store no more than three payment cards.");
        }

        PaymentCard card = new PaymentCard();
        card.setUserId(userId);
        applyCardDetails(card, request, true);
        card = paymentCardRepository.save(card);

        emailService.sendProfileChangedEmail(user.getEmail(), user.getFirstName());
        return toCardResponse(card);
    }

    @Transactional
    public PaymentCardResponse updatePaymentCard(Long userId, Long cardId, PaymentCardRequest request) {
        User user = requireUser(userId);
        PaymentCard card = paymentCardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new AccountOperationException("Payment card not found."));

        applyCardDetails(card, request, false);
        card = paymentCardRepository.save(card);
        emailService.sendProfileChangedEmail(user.getEmail(), user.getFirstName());
        return toCardResponse(card);
    }

    @Transactional
    public void deletePaymentCard(Long userId, Long cardId) {
        User user = requireUser(userId);
        PaymentCard card = paymentCardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new AccountOperationException("Payment card not found."));
        paymentCardRepository.delete(card);
        emailService.sendProfileChangedEmail(user.getEmail(), user.getFirstName());
    }

    private boolean upsertAddress(Long userId, AddressRequest request) {
        Address address = addressRepository.findByUserId(userId).orElseGet(() -> {
            Address newAddress = new Address();
            newAddress.setUserId(userId);
            return newAddress;
        });

        String line1 = request.streetLine1().trim();
        String line2 = normalizeOptional(request.streetLine2());
        String city = request.city().trim();
        String state = request.state().trim();
        String postalCode = request.postalCode().trim();
        String country = request.country().trim();

        boolean changed = address.getId() == null
                || !Objects.equals(address.getStreetLine1(), line1)
                || !Objects.equals(address.getStreetLine2(), line2)
                || !Objects.equals(address.getCity(), city)
                || !Objects.equals(address.getState(), state)
                || !Objects.equals(address.getPostalCode(), postalCode)
                || !Objects.equals(address.getCountry(), country);

        if (changed) {
            address.setStreetLine1(line1);
            address.setStreetLine2(line2);
            address.setCity(city);
            address.setState(state);
            address.setPostalCode(postalCode);
            address.setCountry(country);
            addressRepository.save(address);
        }
        return changed;
    }

    private void applyCardDetails(PaymentCard card, PaymentCardRequest request, boolean requireCardNumber) {
        card.setCardholderName(request.cardholderName().trim());
        card.setExpirationMonth(request.expirationMonth());
        card.setExpirationYear(request.expirationYear());

        boolean hasCardNumber = request.cardNumber() != null && !request.cardNumber().isBlank();
        if (!hasCardNumber) {
            if (requireCardNumber) {
                throw new AccountOperationException("Enter a valid payment card number.");
            }
            cardEncryptionService.validateExpiration(request.expirationMonth(), request.expirationYear());
            return;
        }

        CardEncryptionService.ValidatedCard validated = cardEncryptionService.validate(
                request.cardNumber(), request.expirationMonth(), request.expirationYear());
        CardEncryptionService.EncryptedCard encrypted = cardEncryptionService.encrypt(validated.cardNumber());
        card.setCardBrand(validated.brand());
        card.setLastFour(validated.lastFour());
        card.setEncryptedCardData(encrypted.ciphertext());
        card.setEncryptionIv(encrypted.iv());
    }

    private ProfileResponse toResponse(User user) {
        AddressResponse address = addressRepository.findByUserId(user.getId())
                .map(this::toAddressResponse)
                .orElse(null);
        List<PaymentCardResponse> cards = paymentCardRepository.findByUserIdOrderByIdAsc(user.getId())
                .stream().map(this::toCardResponse).toList();
        return new ProfileResponse(
                user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                user.getPhone(), user.getPromotionsOptIn(), address, cards);
    }

    private AddressResponse toAddressResponse(Address address) {
        return new AddressResponse(
                address.getId(), address.getStreetLine1(), address.getStreetLine2(),
                address.getCity(), address.getState(), address.getPostalCode(), address.getCountry());
    }

    private PaymentCardResponse toCardResponse(PaymentCard card) {
        return new PaymentCardResponse(
                card.getId(), card.getCardholderName(), card.getCardBrand(), card.getLastFour(),
                card.getExpirationMonth(), card.getExpirationYear());
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(AuthenticationRequiredException::new);
    }

    private User requireLockedUser(Long userId) {
        return userLockRepository.findByIdForUpdate(userId).orElseThrow(AuthenticationRequiredException::new);
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
