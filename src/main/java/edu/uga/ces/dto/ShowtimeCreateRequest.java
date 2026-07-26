package edu.uga.ces.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Body for POST /api/admin/showtimes.
 */
public record ShowtimeCreateRequest(
        @NotNull @Positive Long movieId,
        @NotNull @FutureOrPresent LocalDate date,
        @NotNull LocalTime time,
        @NotNull @Positive Long showroomId
) {
}
