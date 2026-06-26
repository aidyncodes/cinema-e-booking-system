package edu.uga.ces.dto;

import java.util.List;

/**
 * Info for the movie detail page (total info). Adds description, trailer, and
 * showtimes (as a real list) on top of the summary fields.
 */
public record MovieDetail(
        Long id,
        String title,
        String genre,
        String rating,
        String description,
        String posterUrl,
        String trailerUrl,
        String status,
        List<String> showtimes
) {}
