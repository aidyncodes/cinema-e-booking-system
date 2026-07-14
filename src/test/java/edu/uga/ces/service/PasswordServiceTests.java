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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordServiceTests {

    private UserRepository userRepository;
    private PasswordResetTokenRepository resetTokenRepository;
    private PasswordEncoder passwordEncoder;
    private AccountEmailService emailService;
    private PasswordService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        resetTokenRepository = mock(PasswordResetTokenRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        emailService = mock(AccountEmailService.class);
        service = new PasswordService(userRepository, resetTokenRepository, passwordEncoder, emailService);
    }

    @Test
    void forgotPasswordDoesNotRevealOrCreateTokenForUnknownEmail() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        service.forgotPassword(new ForgotPasswordRequest("Missing@Example.com"));

        verify(resetTokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyString());
    }

    @Test
    void resetPasswordHashesPasswordAndConsumesAllOpenTokens() throws Exception {
        String rawToken = "one-time-reset-token";
        User user = user(7L);
        PasswordResetToken token = new PasswordResetToken();
        token.setUserId(7L);
        token.setTokenHash(sha256(rawToken));
        token.setExpiresAt(Instant.now().plusSeconds(600));

        when(resetTokenRepository.findByTokenHash(sha256(rawToken))).thenReturn(Optional.of(token));
        when(resetTokenRepository.findByUserIdAndUsedAtIsNull(7L)).thenReturn(List.of(token));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewPassword123!")).thenReturn("new-bcrypt-hash");

        service.resetPassword(new ResetPasswordRequest(rawToken, "NewPassword123!"));

        assertEquals("new-bcrypt-hash", user.getPasswordHash());
        assertNotNull(token.getUsedAt());
        verify(userRepository).save(user);
        verify(resetTokenRepository).saveAll(List.of(token));
        verify(emailService).sendPasswordChangedEmail(user.getEmail(), user.getFirstName());
    }

    @Test
    void rejectsExpiredResetToken() throws Exception {
        String rawToken = "expired-token";
        PasswordResetToken token = new PasswordResetToken();
        token.setExpiresAt(Instant.now().minusSeconds(1));
        when(resetTokenRepository.findByTokenHash(sha256(rawToken))).thenReturn(Optional.of(token));

        assertThrows(InvalidTokenException.class,
                () -> service.resetPassword(new ResetPasswordRequest(rawToken, "NewPassword123!")));
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePasswordRequiresCorrectCurrentPassword() {
        User user = user(9L);
        when(userRepository.findById(9L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", user.getPasswordHash())).thenReturn(false);

        assertThrows(AccountOperationException.class,
                () -> service.changePassword(9L,
                        new ChangePasswordRequest("wrong-password", "NewPassword123!")));
        verify(userRepository, never()).save(any());
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("user@example.com");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setPasswordHash("old-bcrypt-hash");
        user.setPromotionsOptIn(false);
        return user;
    }

    private String sha256(String value) throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
}
