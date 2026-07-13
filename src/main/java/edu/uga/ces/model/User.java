package edu.uga.ces.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * User account entity, one row per registered user (customer or admin).
 * Manual getters/setters to match the style used by Movie (no Lombok in this project).
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "first_name", nullable = false) // specify col name in sql database table
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String phone;

    // ACTIVE or INACTIVE 
    @Column(nullable = false)
    private String status;

    // CUSTOMER or ADMIN
    @Column(nullable = false)
    private String role;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "promotions_opt_in", nullable = false)
    private Boolean promotionsOptIn;

    // Set at registration, but cleared once account is confirmed w/ email.
    @Column(name = "confirmation_token")
    private String confirmationToken;

    @Column(name = "confirmation_token_expires_at")
    private Instant confirmationTokenExpiresAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Boolean getPromotionsOptIn() { return promotionsOptIn; }
    public void setPromotionsOptIn(Boolean promotionsOptIn) { this.promotionsOptIn = promotionsOptIn; }

    public String getConfirmationToken() { return confirmationToken; }
    public void setConfirmationToken(String confirmationToken) { this.confirmationToken = confirmationToken; }

    public Instant getConfirmationTokenExpiresAt() { return confirmationTokenExpiresAt; }
    public void setConfirmationTokenExpiresAt(Instant confirmationTokenExpiresAt) { this.confirmationTokenExpiresAt = confirmationTokenExpiresAt; }
}