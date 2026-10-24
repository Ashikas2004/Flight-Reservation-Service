package com.flightreservation.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class FlightSearchRequest {

    @NotBlank(message = "Origin airport code is required")
    @Size(min = 3, max = 3, message = "Origin must be a 3-letter IATA code")
    private String origin;

    @NotBlank(message = "Destination airport code is required")
    @Size(min = 3, max = 3, message = "Destination must be a 3-letter IATA code")
    private String destination;

    @NotNull(message = "Departure date is required")
    private LocalDate departureDate;

    @Min(value = 1, message = "At least 1 passenger required")
    private int passengers = 1;

    private int page = 0;
    private int size = 10;
    private String sortBy = "departureTime";
    private String sortDir = "asc";
}
