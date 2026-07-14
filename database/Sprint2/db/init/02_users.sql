CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(60) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(25),
    status VARCHAR(20) NOT NULL DEFAULT 'INACTIVE',
    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    promotions_opt_in BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_users_role CHECK (role IN ('CUSTOMER', 'ADMIN'))
);

CREATE TABLE IF NOT EXISTS addresses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    street_line1 VARCHAR(255) NOT NULL,
    street_line2 VARCHAR(255),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(50) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    country VARCHAR(100) NOT NULL DEFAULT 'USA',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_addresses_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS payment_cards (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    cardholder_name VARCHAR(150) NOT NULL,
    card_brand VARCHAR(50),
    last_four VARCHAR(4) NOT NULL,
    expiration_month TINYINT NOT NULL,
    expiration_year SMALLINT NOT NULL,
    encrypted_card_data VARBINARY(1024) NOT NULL,
    encryption_iv VARBINARY(16),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_cards_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT chk_payment_cards_expiration_month
        CHECK (expiration_month BETWEEN 1 AND 12),
    CONSTRAINT chk_payment_cards_last_four
        CHECK (CHAR_LENGTH(last_four) = 4)
);

CREATE TABLE IF NOT EXISTS favorites (
    user_id BIGINT NOT NULL,
    movie_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, movie_id),
    CONSTRAINT fk_favorites_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_favorites_movie
        FOREIGN KEY (movie_id) REFERENCES movies(id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS email_confirmation_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_email_confirmation_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_password_reset_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_payment_cards_user_id ON payment_cards(user_id);
CREATE INDEX idx_favorites_movie_id ON favorites(movie_id);
CREATE INDEX idx_email_confirmation_tokens_user_id ON email_confirmation_tokens(user_id);
CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens(user_id);

-- Demo seed users. Plaintext test password for all three accounts: Password123!
INSERT IGNORE INTO users
(id, email, password_hash, first_name, last_name, phone, status, role, promotions_opt_in)
VALUES
(
    1,
    'admin@cinema.com',
    '$2a$10$HYBHN1d0x8L8MKV7/G/vj.v7OwYG9sMC.bHpUVNqyhs6EZCpMkvZm',
    'Admin',
    'User',
    '706-555-0100',
    'ACTIVE',
    'ADMIN',
    FALSE
),
(
    2,
    'customer@cinema.com',
    '$2a$10$HYBHN1d0x8L8MKV7/G/vj.v7OwYG9sMC.bHpUVNqyhs6EZCpMkvZm',
    'Demo',
    'Customer',
    '706-555-0101',
    'ACTIVE',
    'CUSTOMER',
    TRUE
),
(
    3,
    'inactive@cinema.com',
    '$2a$10$HYBHN1d0x8L8MKV7/G/vj.v7OwYG9sMC.bHpUVNqyhs6EZCpMkvZm',
    'Inactive',
    'Customer',
    '706-555-0102',
    'INACTIVE',
    'CUSTOMER',
    FALSE
);

INSERT IGNORE INTO addresses
(id, user_id, street_line1, street_line2, city, state, postal_code, country)
VALUES
(
    1,
    2,
    '100 Baxter Street',
    'Apt 12',
    'Athens',
    'GA',
    '30602',
    'USA'
);

INSERT INTO payment_cards
(user_id, cardholder_name, card_brand, last_four, expiration_month, expiration_year, encrypted_card_data, encryption_iv)
SELECT
    2,
    'Demo Customer',
    'Visa',
    '1111',
    12,
    2030,
    UNHEX('8F4D2A91C0B77433A1056C92EF019D4A'),
    UNHEX('00112233445566778899AABBCCDDEEFF')
WHERE NOT EXISTS (
    SELECT 1 FROM payment_cards WHERE user_id = 2 AND last_four = '1111'
);

INSERT INTO payment_cards
(user_id, cardholder_name, card_brand, last_four, expiration_month, expiration_year, encrypted_card_data, encryption_iv)
SELECT
    2,
    'Demo Customer',
    'Mastercard',
    '4444',
    10,
    2029,
    UNHEX('4A9D10F23B7E6C1180AFCC42259D778E'),
    UNHEX('FFEEDDCCBBAA99887766554433221100')
WHERE NOT EXISTS (
    SELECT 1 FROM payment_cards WHERE user_id = 2 AND last_four = '4444'
);

INSERT INTO payment_cards
(user_id, cardholder_name, card_brand, last_four, expiration_month, expiration_year, encrypted_card_data, encryption_iv)
SELECT
    2,
    'Demo Customer',
    'Discover',
    '0005',
    8,
    2031,
    UNHEX('B18A6D4F9210CC77E533A9F04612D8AB'),
    UNHEX('102132435465768798A9BACBDCEDFE0F')
WHERE NOT EXISTS (
    SELECT 1 FROM payment_cards WHERE user_id = 2 AND last_four = '0005'
);

INSERT IGNORE INTO favorites
(user_id, movie_id)
VALUES
(2, 1),
(2, 3);
