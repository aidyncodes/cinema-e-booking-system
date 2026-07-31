package edu.uga.ces.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * One held or booked seat for a single showtime, mapping the seat_reservations
 * table created in Sprint 3. Guests hold seats using their HttpSession id before
 * login; user_id is filled in once they authenticate at checkout.
 */
@Entity
@Table(
        name = "seat_reservations",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_seat_reservations_showtime_seat",
                columnNames = {"showtime_id", "seat_label"}
        )
)
public class SeatReservation {

    public static final String STATUS_HELD = "HELD";
    public static final String STATUS_BOOKED = "BOOKED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "showtime_id", nullable = false)
    private Long showtimeId;

    @Column(name = "seat_label", nullable = false, length = 10)
    private String seatLabel;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "ticket_type", nullable = false, length = 20)
    private String ticketType;

    @Column(name = "session_id", length = 128)
    private String sessionId;

    @Column(name = "user_id")
    private Long userId;

    // Set by the database default; read-only from the app's point of view.
    @Column(name = "held_at", insertable = false, updatable = false)
    private Instant heldAt;

    // The column existed in the schema from the start, but nothing set or read
    // it: holds never expired. BookingService now sets this when a hold is
    // created, and SeatHoldCleanupJob deletes rows once it's passed.
    @Column(name = "expires_at")
    private Instant expiresAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getShowtimeId() {
        return showtimeId;
    }

    public void setShowtimeId(Long showtimeId) {
        this.showtimeId = showtimeId;
    }

    public String getSeatLabel() {
        return seatLabel;
    }

    public void setSeatLabel(String seatLabel) {
        this.seatLabel = seatLabel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTicketType() {
        return ticketType;
    }

    public void setTicketType(String ticketType) {
        this.ticketType = ticketType;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Instant getHeldAt() {
        return heldAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}