package edu.uga.ces.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "payment_cards")
public class PaymentCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "cardholder_name", nullable = false)
    private String cardholderName;

    @Column(name = "card_brand")
    private String cardBrand;

    @Column(name = "last_four", nullable = false, length = 4)
    private String lastFour;

    @Column(name = "expiration_month", nullable = false)
    private int expirationMonth;

    @Column(name = "expiration_year", nullable = false)
    private int expirationYear;

    @Column(name = "encrypted_card_data", nullable = false)
    private byte[] encryptedCardData;

    @Column(name = "encryption_iv", nullable = false)
    private byte[] encryptionIv;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getCardholderName() { return cardholderName; }
    public void setCardholderName(String cardholderName) { this.cardholderName = cardholderName; }
    public String getCardBrand() { return cardBrand; }
    public void setCardBrand(String cardBrand) { this.cardBrand = cardBrand; }
    public String getLastFour() { return lastFour; }
    public void setLastFour(String lastFour) { this.lastFour = lastFour; }
    public int getExpirationMonth() { return expirationMonth; }
    public void setExpirationMonth(int expirationMonth) { this.expirationMonth = expirationMonth; }
    public int getExpirationYear() { return expirationYear; }
    public void setExpirationYear(int expirationYear) { this.expirationYear = expirationYear; }
    public byte[] getEncryptedCardData() { return encryptedCardData; }
    public void setEncryptedCardData(byte[] encryptedCardData) { this.encryptedCardData = encryptedCardData; }
    public byte[] getEncryptionIv() { return encryptionIv; }
    public void setEncryptionIv(byte[] encryptionIv) { this.encryptionIv = encryptionIv; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
