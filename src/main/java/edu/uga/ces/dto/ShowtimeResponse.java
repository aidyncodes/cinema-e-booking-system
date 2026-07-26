package edu.uga.ces.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record ShowtimeResponse(
        Long id,
        Long movieId,
        String movieTitle,
        LocalDate date,
        LocalTime time,
        ShowroomResponse showroom
) {
}
