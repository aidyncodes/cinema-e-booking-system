CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    confirmation_number VARCHAR(40) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    showtime_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    subtotal DECIMAL(10,2) NOT NULL,
    tax_amount DECIMAL(10,2) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    confirmation_email VARCHAR(255) NOT NULL,
    payment_card_brand VARCHAR(50),
    payment_card_last_four VARCHAR(4),
    placed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_orders_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_orders_showtime
        FOREIGN KEY (showtime_id) REFERENCES showtimes(id)
        ON DELETE RESTRICT,
    CONSTRAINT uq_orders_id_showtime
        UNIQUE (id, showtime_id),
    CONSTRAINT chk_orders_status
        CHECK (status IN ('PENDING', 'PAID', 'CANCELLED')),
    CONSTRAINT chk_orders_amounts
        CHECK (
            subtotal >= 0
            AND tax_amount >= 0
            AND total_amount = subtotal + tax_amount
        ),
    CONSTRAINT chk_orders_card_last_four
        CHECK (
            payment_card_last_four IS NULL
            OR CHAR_LENGTH(payment_card_last_four) = 4
        ),
    CONSTRAINT chk_orders_paid_fields
        CHECK (
            status <> 'PAID'
            OR (
                placed_at IS NOT NULL
                AND payment_card_brand IS NOT NULL
                AND payment_card_last_four IS NOT NULL
            )
        )
);

CREATE TABLE IF NOT EXISTS tickets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    showtime_id BIGINT NOT NULL,
    seat_label VARCHAR(10) NOT NULL,
    ticket_type VARCHAR(20) NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tickets_order_showtime
        FOREIGN KEY (order_id, showtime_id)
        REFERENCES orders(id, showtime_id)
        ON DELETE CASCADE,
    CONSTRAINT uq_tickets_showtime_seat
        UNIQUE (showtime_id, seat_label),
    CONSTRAINT chk_tickets_ticket_type
        CHECK (ticket_type IN ('ADULT', 'CHILD', 'SENIOR')),
    CONSTRAINT chk_tickets_unit_price
        CHECK (unit_price >= 0)
);

CREATE TABLE IF NOT EXISTS payment_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    transaction_reference VARCHAR(100) NOT NULL UNIQUE,
    card_brand VARCHAR(50) NOT NULL,
    card_last_four VARCHAR(4) NOT NULL,
    processed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_transactions_order
        FOREIGN KEY (order_id) REFERENCES orders(id)
        ON DELETE CASCADE,
    CONSTRAINT chk_payment_transactions_status
        CHECK (status IN ('PENDING', 'APPROVED', 'DECLINED')),
    CONSTRAINT chk_payment_transactions_amount
        CHECK (amount >= 0),
    CONSTRAINT chk_payment_transactions_last_four
        CHECK (CHAR_LENGTH(card_last_four) = 4),
    CONSTRAINT chk_payment_transactions_processed
        CHECK (status = 'PENDING' OR processed_at IS NOT NULL)
);

CREATE INDEX idx_orders_user_placed
    ON orders(user_id, placed_at);
CREATE INDEX idx_orders_showtime
    ON orders(showtime_id);
CREATE INDEX idx_tickets_order
    ON tickets(order_id);
CREATE INDEX idx_payment_transactions_order
    ON payment_transactions(order_id);

-- Paid order history for the stored-card customer.
INSERT INTO orders
    (id, confirmation_number, user_id, showtime_id, status,
     subtotal, tax_amount, total_amount, confirmation_email,
     payment_card_brand, payment_card_last_four, placed_at)
VALUES
    (1, 'CES-20260728-0001', 2, 1, 'PAID',
     18.00, 1.44, 19.44, 'customer@cinema.com',
     'Visa', '1111', '2026-07-28 10:00:00'),
    (2, 'CES-20260728-0002', 2, 3, 'PAID',
     12.00, 0.96, 12.96, 'customer@cinema.com',
     'Mastercard', '4444', '2026-07-28 11:00:00');

INSERT INTO tickets
    (id, order_id, showtime_id, seat_label, ticket_type, unit_price)
VALUES
    (1, 1, 1, 'A1', 'ADULT', 12.00),
    (2, 1, 1, 'A2', 'CHILD', 6.00),
    (3, 2, 3, 'C5', 'ADULT', 12.00);

INSERT INTO payment_transactions
    (id, order_id, status, amount, transaction_reference,
     card_brand, card_last_four, processed_at)
VALUES
    (1, 1, 'APPROVED', 19.44, 'DEMO-TXN-0001',
     'Visa', '1111', '2026-07-28 10:00:00'),
    (2, 2, 'APPROVED', 12.96, 'DEMO-TXN-0002',
     'Mastercard', '4444', '2026-07-28 11:00:00');
