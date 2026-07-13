package edu.uga.ces.controller;

import edu.uga.ces.dto.AuthResponse;
import edu.uga.ces.dto.LoginRequest;
import edu.uga.ces.dto.RegisterRequest;
import edu.uga.ces.exception.InvalidTokenException;
import edu.uga.ces.model.User;
import edu.uga.ces.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Registration successful. Check your email to confirm your account."));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        User user = userService.login(request);

        // Fresh session + rotated session id on every successful login, so a
        // session id issued before login (e.g. session fixation) can't be reused.
        HttpSession session = httpRequest.getSession(true);
        httpRequest.changeSessionId();
        session.setAttribute("userId", user.getId());
        session.setAttribute("role", user.getRole());

        return ResponseEntity.ok(new AuthResponse(
                user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getRole()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false); // don't create one just to kill it
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully."));
    }

    // Meant to be opened directly from the email link in a browser, so it
    // returns a small HTML page rather than JSON.
    @GetMapping(value = "/confirm", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> confirm(@RequestParam String token) {
        try {
            userService.confirmEmail(token);
            return ResponseEntity.ok(htmlPage("Your account is confirmed!", "You can close this tab and log in at CES Cinema."));
        } catch (InvalidTokenException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(htmlPage("Confirmation failed", ex.getMessage()));
        }
    }

    private String htmlPage(String heading, String body) {
        return """
                <!DOCTYPE html>
                <html>
                <head><title>%s</title></head>
                <body style="font-family: Arial, sans-serif; text-align: center; padding: 60px;">
                    <h1>%s</h1>
                    <p>%s</p>
                </body>
                </html>
                """.formatted(heading, heading, body);
    }
}