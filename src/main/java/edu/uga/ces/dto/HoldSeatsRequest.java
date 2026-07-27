package edu.uga.ces.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request to hold seats for a showtime. The seat labels are the seats the user
 * clicked; the ticket counts are the quantities chosen per age category. The
 * service checks that the seat count equals the total ticket count.
 */
public record HoldSeatsRequest(
        @NotEmpty(message = "Select at least one seat.")
        List<String> seats,

        @Min(value = 0, message = "Adult count cannot be negative.")
        int adultCount,

        @Min(value = 0, message = "Senior count cannot be negative.")
        int seniorCount,

        @Min(value = 0, message = "Child count cannot be negative.")
        int childCount
) {
}
