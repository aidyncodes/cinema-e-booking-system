package edu.uga.ces.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * One row per confirmation link emailed to a user. Stores a SHA-256 hash of
 * the token, never the raw value, same principle as password hashing, but
 * SHA-256 (not BCrypt) is a better tool here (i think): the raw token is already a
 * random UUID with far more entropy than a human password, so it doesn't need
 * a slow hash to resist guessing. It just needs to not be reconstructible
 * if the DB leaks.
 */
@Entity
@Table(name = "email_confirmation_tokens")
public class EmailConfirmationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getUsedAt() { return usedAt; }
    public void setUsedAt(Instant usedAt) { this.usedAt = usedAt; }

    public Instant getCreatedAt() { return createdAt; }
}