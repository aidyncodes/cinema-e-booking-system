package edu.uga.ces.controller;

import edu.uga.ces.dto.MovieCreateRequest;
import edu.uga.ces.dto.MovieDetail;
import edu.uga.ces.exception.AdminAccessRequiredException;
import edu.uga.ces.service.MovieService;
import edu.uga.ces.service.SessionUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only movie management. Kept separate from the public MovieController
 * (/api/movies) the same way AccountController sits apart from AuthController -
 * different owner, different concern, both backed by the same MovieService.
 */
@RestController
@RequestMapping("/api/admin/movies")
public class AdminMovieController {

    private final MovieService movieService;
    private final SessionUserService sessionUserService;

    public AdminMovieController(MovieService movieService, SessionUserService sessionUserService) {
        this.movieService = movieService;
        this.sessionUserService = sessionUserService;
    }

    @PostMapping
    public ResponseEntity<MovieDetail> addMovie(@Valid @RequestBody MovieCreateRequest request,
                                                 HttpServletRequest httpRequest) {
        requireAdmin(httpRequest);
        MovieDetail created = movieService.createMovie(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // SessionUserService.requireUserId only confirms *someone* is logged in.
    // AuthController#login also stores "role" on the session at login time, so
    // admin endpoints check it directly here rather than introducing a new
    // shared service for a single role check.
    private void requireAdmin(HttpServletRequest httpRequest) {
        sessionUserService.requireUserId(httpRequest); // throws AuthenticationRequiredException if not logged in

        HttpSession session = httpRequest.getSession(false);
        Object role = session.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            throw new AdminAccessRequiredException();
        }
    }
}