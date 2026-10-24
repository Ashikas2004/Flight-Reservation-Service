package com.flightreservation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AirlineUpdateRequest {

    @NotBlank(message = "Airline name is required")
    @Size(max = 100)
    private String name;

    @Size(max = 100)
    private String country;

    private String logoUrl;
}
