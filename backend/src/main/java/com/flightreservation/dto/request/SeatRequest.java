package com.flightreservation.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SeatRequest {

    @NotNull(message = "Flight ID is required")
    private Long flightId;

    @NotBlank(message = "Seat number is required")
    @Size(max = 5)
    private String seatNumber;

    @NotBlank(message = "Seat class is required")
    private String seatClass;

    @DecimalMin(value = "0.00", message = "Extra price cannot be negative")
    private BigDecimal extraPrice = BigDecimal.ZERO;
}
