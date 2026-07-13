package edu.uga.ces.repository;

import edu.uga.ces.model.EmailConfirmationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailConfirmationTokenRepository extends JpaRepository<EmailConfirmationToken, Long> {
    Optional<EmailConfirmationToken> findByTokenHash(String tokenHash);
}