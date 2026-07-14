package edu.uga.ces.service;

import edu.uga.ces.exception.AccountOperationException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CardEncryptionServiceTests {

    private static final String TEST_KEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

    private final CardEncryptionService service = new CardEncryptionService(TEST_KEY);

    @Test
    void encryptsAndDecryptsValidatedCardWithoutStoringPlaintext() {
        CardEncryptionService.ValidatedCard card = service.validate("4111 1111 1111 1111", 12, 2030);
        CardEncryptionService.EncryptedCard encrypted = service.encrypt(card.cardNumber());

        assertEquals("1111", card.lastFour());
        assertEquals("Visa", card.brand());
        assertFalse(Arrays.equals(
                card.cardNumber().getBytes(StandardCharsets.UTF_8), encrypted.ciphertext()));
        assertEquals(card.cardNumber(), service.decrypt(encrypted.ciphertext(), encrypted.iv()));
    }

    @Test
    void rejectsInvalidCardNumber() {
        assertThrows(AccountOperationException.class,
                () -> service.validate("4111111111111112", 12, 2030));
    }

    @Test
    void refusesEncryptionWhenKeyIsMissing() {
        CardEncryptionService unconfigured = new CardEncryptionService("");
        assertThrows(IllegalStateException.class, () -> unconfigured.encrypt("4111111111111111"));
    }
}
