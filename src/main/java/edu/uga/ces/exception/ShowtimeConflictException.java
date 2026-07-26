package edu.uga.ces.exception;

import java.time.LocalDate;
import java.time.LocalTime;

public class ShowtimeConflictException extends RuntimeException {

    public ShowtimeConflictException(Long showroomId, LocalDate date, LocalTime time) {
        super("Showroom " + showroomId + " already has a showtime at " + date + " " + time);
    }
}
