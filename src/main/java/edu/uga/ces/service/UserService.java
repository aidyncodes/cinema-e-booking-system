package edu.uga.ces.service;

import edu.uga.ces.dto.LoginRequest;
import edu.uga.ces.dto.RegisterRequest;
import edu.uga.ces.exception.AccountOperationException;
import edu.uga.ces.exception.AccountNotActiveException;
import edu.uga.ces.exception.EmailAlreadyExistsException;
import edu.uga.ces.exception.InvalidCredentialsException;
import edu.uga.ces.exception.InvalidTokenException;
import edu.uga.ces.model.EmailConfirmationToken;
import edu.uga.ces.model.User;
import edu.uga.ces.repository.EmailConfirmationTokenRepository;
import edu.uga.ces.repository.UserRepository;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;

/**
 * controller asks service for data; the service uses the repo to hit the DB
 * (same shape as MovieService). Servlet-session mechanics (HttpSession) stay
 * in the controller so this class is plain and easy to unit test.
 */
@Service
public class UserService {

    private static final String ACTIVE = "ACTIVE";
    private static final String INACTIVE = "INACTIVE";
    private static final String ROLE_CUSTOMER = "CUSTOMER";
    private static final int CONFIRMATION_TOKEN_VALID_HOURS = 24;

    private final UserRepository userRepository;
    private final EmailConfirmationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public UserService(UserRepository userRepository,
                        EmailConfirmationTokenRepository tokenRepository,
                        PasswordEncoder passwordEncoder,
                        EmailService emailService) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional
    public void register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        User user = new User();
        user.setEmail(email);
        user.setFirstName(req.firstName().trim());
        user.setLastName(req.lastName().trim());
        user.setPhone(req.phone() == null ? null : req.phone().trim());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setStatus(INACTIVE);
        user.setRole(ROLE_CUSTOMER); // never trust a role from the client, see RegisterRequest
        user.setPromotionsOptIn(Boolean.TRUE.equals(req.promotionsOptIn()));

        user = userRepository.save(user); // IDENTITY strategy, id is populated immediately

        String rawToken = UUID.randomUUID().toString();

        EmailConfirmationToken token = new EmailConfirmationToken();
        token.setUserId(user.getId());
        token.setTokenHash(hashToken(rawToken));
        token.setExpiresAt(Instant.now().plus(CONFIRMATION_TOKEN_VALID_HOURS, ChronoUnit.HOURS));
        tokenRepository.save(token);

        try {
            // Raw token goes in the email; only its hash ever touches the DB.
            emailService.sendConfirmationEmail(user.getEmail(), user.getFirstName(), rawToken);
        } catch (MailException ex) {
            throw new AccountOperationException("Could not send confirmation email. Please try again in a moment.");
        }
    }

    // Returns the authenticated user; throws before returning if anything is wrong.
    // Controller is responsible for turning this into a session.
    public User login(LoginRequest req) {
        String email = req.email().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("No account found with that email address."));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Incorrect password.");
        }

        if (!ACTIVE.equals(user.getStatus())) {
            throw new AccountNotActiveException("Please confirm your email before logging in.");
        }

        return user;
    }

    public void confirmEmail(String rawToken) {
        String hash = hashToken(rawToken);

        EmailConfirmationToken token = tokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new InvalidTokenException("This confirmation link is invalid."));

        if (token.getUsedAt() != null) {
            throw new InvalidTokenException("This confirmation link has already been used.");
        }

        if (Instant.now().isAfter(token.getExpiresAt())) {
            throw new InvalidTokenException("This confirmation link has expired.");
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new InvalidTokenException("This confirmation link is invalid."));

        user.setStatus(ACTIVE);
        userRepository.save(user);

        token.setUsedAt(Instant.now());
        tokenRepository.save(token);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed present on every standard JVM; this never actually runs.
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
