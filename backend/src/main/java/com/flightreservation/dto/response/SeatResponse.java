package com.flightreservation.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatResponse {
    private Long id;
    private String seatNumber;
    private String seatClass;
    private String status;
    private BigDecimal extraPrice;
    private Long flightId;

    @JsonProperty("isLockedByCurrentUser")
    private Boolean isLockedByCurrentUser;
}

