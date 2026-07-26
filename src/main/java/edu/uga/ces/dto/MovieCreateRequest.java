package edu.uga.ces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Body for POST /api/admin/movies.
 *
 * No schema change needed - the movies table already has a column for every
 * field here (see 01_movies.sql / Movie.java).
 *
 * status is restricted to the two values MovieService actually filters on
 * (CURRENTLY_RUNNING, COMING_SOON) so a typo here can't silently make a movie
 * invisible on the home page or "coming soon" section.
 */
public record MovieCreateRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 100) String genre,
        @NotBlank
        @Pattern(regexp = "CURRENTLY_RUNNING|COMING_SOON",
                message = "status must be CURRENTLY_RUNNING or COMING_SOON")
        String status,
        @Size(max = 20) String rating,
        String description,
        @Size(max = 1000) String posterUrl,
        @Size(max = 1000) String trailerUrl,
        List<String> showtimes
) {}