package edu.uga.ces.service;

import edu.uga.ces.exception.AccountOperationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.YearMonth;
import java.util.Base64;

@Service
public class CardEncryptionService {

    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_IV_BYTES = 12;

    private final SecureRandom secureRandom = new SecureRandom();
    private final SecretKey secretKey;

    public CardEncryptionService(@Value("${app.card-encryption-key:}") String encodedKey) {
        this.secretKey = parseKey(encodedKey);
    }

    public ValidatedCard validate(String rawCardNumber, int expirationMonth, int expirationYear) {
        String cardNumber = rawCardNumber == null ? "" : rawCardNumber.replaceAll("[\\s-]", "");
        if (!cardNumber.matches("\\d{13,19}") || !passesLuhn(cardNumber)) {
            throw new AccountOperationException("Enter a valid payment card number.");
        }

        validateExpiration(expirationMonth, expirationYear);

        return new ValidatedCard(
                cardNumber,
                cardNumber.substring(cardNumber.length() - 4),
                detectBrand(cardNumber));
    }

    public void validateExpiration(int expirationMonth, int expirationYear) {
        YearMonth expiration;
        try {
            expiration = YearMonth.of(expirationYear, expirationMonth);
        } catch (RuntimeException ex) {
            throw new AccountOperationException("Enter a valid card expiration date.");
        }
        if (expiration.isBefore(YearMonth.now())) {
            throw new AccountOperationException("The payment card has expired.");
        }
    }

    public EncryptedCard encrypt(String cardNumber) {
        if (secretKey == null) {
            throw new IllegalStateException(
                    "CARD_ENCRYPTION_KEY is not configured. Set it to a Base64-encoded 16, 24, or 32 byte key.");
        }

        byte[] iv = new byte[GCM_IV_BYTES];
        secureRandom.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(cardNumber.getBytes(StandardCharsets.UTF_8));
            return new EncryptedCard(ciphertext, iv);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Payment card encryption failed.", ex);
        }
    }

    String decrypt(byte[] ciphertext, byte[] iv) {
        if (secretKey == null) {
            throw new IllegalStateException("CARD_ENCRYPTION_KEY is not configured.");
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Payment card decryption failed.", ex);
        }
    }

    private SecretKey parseKey(String encodedKey) {
        if (encodedKey == null || encodedKey.isBlank()) {
            return null;
        }
        try {
            byte[] keyBytes = Base64.getDecoder().decode(encodedKey.trim());
            if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
                throw new IllegalArgumentException("invalid AES key length");
            }
            return new SecretKeySpec(keyBytes, "AES");
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "CARD_ENCRYPTION_KEY must be Base64-encoded and decode to 16, 24, or 32 bytes.", ex);
        }
    }

    private boolean passesLuhn(String number) {
        int sum = 0;
        boolean doubleDigit = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = number.charAt(i) - '0';
            if (doubleDigit) {
                digit *= 2;
                if (digit > 9) digit -= 9;
            }
            sum += digit;
            doubleDigit = !doubleDigit;
        }
        return sum % 10 == 0;
    }

    private String detectBrand(String number) {
        if (number.startsWith("4")) return "Visa";
        if (number.matches("^(5[1-5]|2(2[2-9]|[3-6][0-9]|7[01]|720)).*")) return "Mastercard";
        if (number.matches("^3[47].*")) return "American Express";
        if (number.matches("^(6011|65|64[4-9]).*")) return "Discover";
        return "Card";
    }

    public record ValidatedCard(String cardNumber, String lastFour, String brand) {}
    public record EncryptedCard(byte[] ciphertext, byte[] iv) {}
}
