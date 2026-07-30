package edu.uga.ces.dto;

/**
 * Safe payment-card values copied onto an order/payment transaction.
 *
 * Full card data is deliberately excluded. Keeping a brand/last-four snapshot
 * lets order history remain readable if the user later edits or deletes the
 * saved card.
 */
public record PaymentCardSnapshot(
        Long paymentCardId,
        String cardBrand,
        String lastFour
) {}
