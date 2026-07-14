package edu.uga.ces.controller;

import edu.uga.ces.dto.PaymentCardRequest;
import edu.uga.ces.dto.PaymentCardResponse;
import edu.uga.ces.dto.ProfileResponse;
import edu.uga.ces.dto.ProfileUpdateRequest;
import edu.uga.ces.service.ProfileService;
import edu.uga.ces.service.SessionUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;
    private final SessionUserService sessionUserService;

    public ProfileController(ProfileService profileService, SessionUserService sessionUserService) {
        this.profileService = profileService;
        this.sessionUserService = sessionUserService;
    }

    @GetMapping
    public ProfileResponse getProfile(HttpServletRequest request) {
        return profileService.getProfile(sessionUserService.requireUserId(request));
    }

    @PutMapping
    public ProfileResponse updateProfile(@Valid @RequestBody ProfileUpdateRequest body,
                                         HttpServletRequest request) {
        return profileService.updateProfile(sessionUserService.requireUserId(request), body);
    }

    @DeleteMapping("/address")
    public Map<String, String> deleteAddress(HttpServletRequest request) {
        profileService.deleteAddress(sessionUserService.requireUserId(request));
        return Map.of("message", "Address removed successfully.");
    }

    @PostMapping("/cards")
    public ResponseEntity<PaymentCardResponse> addCard(@Valid @RequestBody PaymentCardRequest body,
                                                       HttpServletRequest request) {
        PaymentCardResponse card = profileService.addPaymentCard(
                sessionUserService.requireUserId(request), body);
        return ResponseEntity.status(HttpStatus.CREATED).body(card);
    }

    @PutMapping("/cards/{cardId}")
    public PaymentCardResponse updateCard(@PathVariable Long cardId,
                                          @Valid @RequestBody PaymentCardRequest body,
                                          HttpServletRequest request) {
        return profileService.updatePaymentCard(
                sessionUserService.requireUserId(request), cardId, body);
    }

    @DeleteMapping("/cards/{cardId}")
    public Map<String, String> deleteCard(@PathVariable Long cardId, HttpServletRequest request) {
        profileService.deletePaymentCard(sessionUserService.requireUserId(request), cardId);
        return Map.of("message", "Payment card removed successfully.");
    }
}
