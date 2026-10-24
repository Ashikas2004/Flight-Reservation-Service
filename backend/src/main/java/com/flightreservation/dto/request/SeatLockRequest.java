package com.flightreservation.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SeatLockRequest {

    @NotNull(message = "Flight ID is required")
    private Long flightId;

    @NotEmpty(message = "At least one seat ID must be provided")
    private List<Long> seatIds;
}
