package edu.uga.ces.controller;

import edu.uga.ces.dto.ChangePasswordRequest;
import edu.uga.ces.dto.ForgotPasswordRequest;
import edu.uga.ces.dto.ResetPasswordRequest;
import edu.uga.ces.service.PasswordService;
import edu.uga.ces.service.SessionUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Allen's password endpoints, separate from the existing AuthController. */
@RestController
@RequestMapping("/api/auth")
public class AccountController {

    private final PasswordService passwordService;
    private final SessionUserService sessionUserService;

    public AccountController(PasswordService passwordService, SessionUserService sessionUserService) {
        this.passwordService = passwordService;
        this.sessionUserService = sessionUserService;
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        passwordService.forgotPassword(request);
        return ResponseEntity.ok(Map.of(
                "message", "If an account exists for that email, a password reset link has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        passwordService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password reset successfully. You can now log in."));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest) {
        passwordService.changePassword(sessionUserService.requireUserId(httpRequest), request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully."));
    }
}
