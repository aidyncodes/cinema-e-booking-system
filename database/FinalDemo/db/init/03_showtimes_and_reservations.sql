CREATE TABLE IF NOT EXISTS showrooms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    row_count TINYINT UNSIGNED NOT NULL,
    seats_per_row TINYINT UNSIGNED NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_showrooms_row_count
        CHECK (row_count BETWEEN 1 AND 26),
    CONSTRAINT chk_showrooms_seats_per_row
        CHECK (seats_per_row BETWEEN 1 AND 50)
);

CREATE TABLE IF NOT EXISTS promotions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    promo_code VARCHAR(40) NOT NULL UNIQUE,
    discount_percentage DECIMAL(5,2) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    recipient_count INT UNSIGNED NOT NULL DEFAULT 0,
    sent_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_promotions_discount
        CHECK (discount_percentage > 0 AND discount_percentage <= 100),
    CONSTRAINT chk_promotions_dates
        CHECK (end_date >= start_date)
);

CREATE TABLE IF NOT EXISTS showtimes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    movie_id BIGINT NOT NULL,
    showroom_id BIGINT NOT NULL,
    show_date DATE NOT NULL,
    show_time TIME NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_showtimes_movie
        FOREIGN KEY (movie_id) REFERENCES movies(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_showtimes_showroom
        FOREIGN KEY (showroom_id) REFERENCES showrooms(id)
        ON DELETE RESTRICT,
    CONSTRAINT uq_showtimes_showroom_date_time
        UNIQUE (showroom_id, show_date, show_time)
);

CREATE TABLE IF NOT EXISTS seat_reservations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    showtime_id BIGINT NOT NULL,
    seat_label VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'HELD',
    ticket_type VARCHAR(20) NOT NULL,
    session_id VARCHAR(128),
    user_id BIGINT,
    held_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_seat_reservations_showtime
        FOREIGN KEY (showtime_id) REFERENCES showtimes(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_seat_reservations_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE SET NULL,
    CONSTRAINT uq_seat_reservations_showtime_seat
        UNIQUE (showtime_id, seat_label),
    CONSTRAINT chk_seat_reservations_status
        CHECK (status IN ('HELD', 'BOOKED')),
    CONSTRAINT chk_seat_reservations_ticket_type
        CHECK (ticket_type IN ('ADULT', 'CHILD', 'SENIOR'))
);

CREATE INDEX idx_showtimes_movie_date
    ON showtimes(movie_id, show_date, show_time);
CREATE INDEX idx_showtimes_showroom_date
    ON showtimes(showroom_id, show_date);
CREATE INDEX idx_seat_reservations_session
    ON seat_reservations(session_id);
CREATE INDEX idx_seat_reservations_user
    ON seat_reservations(user_id);

INSERT INTO showrooms (id, name, row_count, seats_per_row)
VALUES
    (1, 'Showroom 1', 5, 8),
    (2, 'Showroom 2', 6, 10),
    (3, 'Showroom 3', 8, 12);

INSERT INTO promotions
    (id, promo_code, discount_percentage, start_date, end_date)
VALUES
    (1, 'SUMMER20', 20.00, '2026-07-24', '2026-08-31');

INSERT INTO showtimes
    (id, movie_id, showroom_id, show_date, show_time)
VALUES
    (1, 1, 1, '2026-07-30', '14:00:00'),
    (2, 1, 2, '2026-07-30', '17:00:00'),
    (3, 2, 3, '2026-07-30', '11:00:00'),
    (4, 3, 1, '2026-07-30', '19:30:00'),
    (5, 4, 2, '2026-07-31', '14:15:00'),
    (6, 5, 3, '2026-07-31', '20:45:00'),
    (7, 6, 1, '2026-08-01', '16:45:00'),
    (8, 7, 2, '2026-08-01', '18:30:00'),
    (9, 8, 3, '2026-08-01', '13:45:00');

-- Demo occupied seats. The HELD row demonstrates a guest reservation before login.
INSERT INTO seat_reservations
    (showtime_id, seat_label, status, ticket_type, session_id, user_id)
VALUES
    (1, 'B4', 'HELD', 'SENIOR', 'demo-guest-session', NULL),
    (3, 'D6', 'HELD', 'ADULT', 'second-demo-session', NULL);
