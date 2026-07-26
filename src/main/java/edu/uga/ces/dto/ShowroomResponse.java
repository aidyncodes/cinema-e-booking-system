package edu.uga.ces.dto;

public record ShowroomResponse(
        Long id,
        String name,
        Integer rowCount,
        Integer seatsPerRow
) {
}
