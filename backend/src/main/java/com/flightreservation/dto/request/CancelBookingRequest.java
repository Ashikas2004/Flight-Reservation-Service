package com.flightreservation.dto.request;

import lombok.Data;

@Data
public class CancelBookingRequest {
    private String reason;
}
