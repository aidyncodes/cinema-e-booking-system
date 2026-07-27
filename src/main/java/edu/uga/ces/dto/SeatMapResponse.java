package edu.uga.ces.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Seat map for one showtime: the showroom layout (rows x seats-per-row) plus the
 * seats that are already taken. The frontend generates the seat buttons from the
 * layout and marks the ones listed in unavailableSeats.
 */
public record SeatMapResponse(
        Long showtimeId,
        Long movieId,
        String movieTitle,
        LocalDate showDate,
        LocalTime showTime,
        ShowroomResponse showroom,
        List<SeatStatus> unavailableSeats
) {
}
