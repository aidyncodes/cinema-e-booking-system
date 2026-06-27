package edu.uga.ces.dto;

/**
 * Movie shape just for list views (home page, search, filter).
 * (not all movie info)
 */
public record MovieSummary(
        Long id,
        String title,
        String genre,
        String rating,
        String posterUrl,
        String status
) {}