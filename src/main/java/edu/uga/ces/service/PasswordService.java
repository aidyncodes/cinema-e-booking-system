package edu.uga.ces.service;

import edu.uga.ces.dto.ChangePasswordRequest;
import edu.uga.ces.dto.ForgotPasswordRequest;
import edu.uga.ces.dto.ResetPasswordRequest;
import edu.uga.ces.exception.AccountOperationException;
import edu.uga.ces.exception.InvalidTokenException;
import edu.uga.ces.model.PasswordResetToken;
import edu.uga.ces.model.User;
import edu.uga.ces.repository.PasswordResetTokenRepository;
import edu.uga.ces.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

/** Password reset/change behavior kept separate from the existing UserService. */
@Service
public class PasswordService {

    private static final int RESET_TOKEN_VALID_HOURS = 1;
    private static final int RESET_TOKEN_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountEmailService emailService;

    public PasswordService(UserRepository userRepository,
                           PasswordResetTokenRepository resetTokenRepository,
                           PasswordEncoder passwordEncoder,
                           AccountEmailService emailService) {
        this.userRepository = userRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.email().trim().toLowerCase();
        userRepository.findByEmail(email).ifPresent(user -> {
            Instant now = Instant.now();
            invalidateOpenResetTokens(user.getId(), now);

            String rawToken = generateSecureToken();
            PasswordResetToken token = new PasswordResetToken();
            token.setUserId(user.getId());
            token.setTokenHash(hashToken(rawToken));
            token.setExpiresAt(now.plus(RESET_TOKEN_VALID_HOURS, ChronoUnit.HOURS));
            resetTokenRepository.save(token);
            emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), rawToken);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = resetTokenRepository.findByTokenHash(hashToken(request.token()))
                .orElseThrow(() -> new InvalidTokenException("This password reset link is invalid."));

        if (token.getUsedAt() != null) {
            throw new InvalidTokenException("This password reset link has already been used.");
        }
        Instant now = Instant.now();
        if (now.isAfter(token.getExpiresAt())) {
            throw new InvalidTokenException("This password reset link has expired.");
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new InvalidTokenException("This password reset link is invalid."));
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        invalidateOpenResetTokens(user.getId(), now);
        emailService.sendPasswordChangedEmail(user.getEmail(), user.getFirstName());
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AccountOperationException("Account not found."));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new AccountOperationException("The current password is incorrect.");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new AccountOperationException("The new password must be different from the current password.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        invalidateOpenResetTokens(user.getId(), Instant.now());
        emailService.sendPasswordChangedEmail(user.getEmail(), user.getFirstName());
    }

    private void invalidateOpenResetTokens(Long userId, Instant usedAt) {
        List<PasswordResetToken> openTokens = resetTokenRepository.findByUserIdAndUsedAtIsNull(userId);
        openTokens.forEach(token -> token.setUsedAt(usedAt));
        if (!openTokens.isEmpty()) resetTokenRepository.saveAll(openTokens);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[RESET_TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
