package edu.uga.ces.controller;

import edu.uga.ces.service.FavoriteService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Favorites endpoints, scoped to the logged-in user.
 *   GET    /api/profile/favorites          - list my favorite movies
 *   POST   /api/profile/favorites/{movieId} - add a movie to my favorites
 *   DELETE /api/profile/favorites/{movieId} - remove a movie from my favorites
 *
 * The user id comes from the session set at login (AuthController). These
 * endpoints self-protect: no session -> 401, regardless of SecurityConfig.
 */
@RestController
@RequestMapping("/api/profile/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    public ResponseEntity<?> list(HttpServletRequest request) {
        Long userId = currentUserId(request);
        if (userId == null) return notAuthenticated();
        return ResponseEntity.ok(favoriteService.getFavorites(userId));
    }

    @PostMapping("/{movieId}")
    public ResponseEntity<?> add(@PathVariable Long movieId, HttpServletRequest request) {
        Long userId = currentUserId(request);
        if (userId == null) return notAuthenticated();
        favoriteService.addFavorite(userId, movieId);
        return ResponseEntity.ok(Map.of("message", "Added to favorites."));
    }

    @DeleteMapping("/{movieId}")
    public ResponseEntity<?> remove(@PathVariable Long movieId, HttpServletRequest request) {
        Long userId = currentUserId(request);
        if (userId == null) return notAuthenticated();
        favoriteService.removeFavorite(userId, movieId);
        return ResponseEntity.ok(Map.of("message", "Removed from favorites."));
    }

    // Reads the logged-in user's id from the session (set at login in AuthController).
    // Returns null when there is no active session / nobody is logged in.
    private Long currentUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return null;
        Object userId = session.getAttribute("userId");
        return (userId instanceof Long) ? (Long) userId : null;
    }

    private ResponseEntity<Map<String, String>> notAuthenticated() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "NOT_AUTHENTICATED",
                        "message", "You must be logged in to manage favorites."));
    }
}
