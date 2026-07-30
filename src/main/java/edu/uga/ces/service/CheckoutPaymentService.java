package edu.uga.ces.service;

import edu.uga.ces.dto.PaymentCardSnapshot;
import edu.uga.ces.exception.AccountOperationException;
import edu.uga.ces.model.PaymentCard;
import edu.uga.ces.repository.PaymentCardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves the saved card selected at checkout.
 *
 * Aidyn's order-finalization service should call requireSavedCard with the
 * authenticated session user and CheckoutPaymentRequest.paymentCardId(), then
 * copy the returned brand and last four digits onto the order and payment
 * transaction.
 */
@Service
public class CheckoutPaymentService {

    private final PaymentCardRepository paymentCardRepository;
    private final CardEncryptionService cardEncryptionService;

    public CheckoutPaymentService(PaymentCardRepository paymentCardRepository,
                                  CardEncryptionService cardEncryptionService) {
        this.paymentCardRepository = paymentCardRepository;
        this.cardEncryptionService = cardEncryptionService;
    }

    @Transactional(readOnly = true)
    public PaymentCardSnapshot requireSavedCard(Long userId, Long paymentCardId) {
        if (userId == null || paymentCardId == null) {
            throw new AccountOperationException("Select a saved payment card.");
        }

        PaymentCard card = paymentCardRepository.findByIdAndUserId(paymentCardId, userId)
                .orElseThrow(() -> new AccountOperationException(
                        "The selected payment card was not found on your account."));

        cardEncryptionService.validateExpiration(
                card.getExpirationMonth(),
                card.getExpirationYear());

        if (card.getCardBrand() == null || card.getCardBrand().isBlank()
                || card.getLastFour() == null || !card.getLastFour().matches("\\d{4}")) {
            throw new AccountOperationException(
                    "The selected payment card cannot be used. Update it in your profile.");
        }

        return new PaymentCardSnapshot(
                card.getId(),
                card.getCardBrand(),
                card.getLastFour());
    }
}
