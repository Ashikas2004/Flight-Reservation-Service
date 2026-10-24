package com.flightreservation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    private Long id;
    private String bookingReference;
    private String status;
    private BigDecimal totalAmount;
    private LocalDateTime bookedAt;
    private LocalDateTime cancelledAt;
    private String cancellationReason;
    private RefundInfo refund;

    private FlightSummary flight;
    private List<PassengerInfo> passengers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundInfo {
        private Long id;
        private BigDecimal refundAmount;
        private String currency;
        private String status;
        private String refundTransactionId;
        private LocalDateTime processedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FlightSummary {
        private Long id;
        private String flightNumber;
        private String airlineName;
        private String originIata;
        private String destinationIata;
        private String originCity;
        private String destinationCity;
        private LocalDateTime departureTime;
        private LocalDateTime arrivalTime;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PassengerInfo {
        private Long id;
        private String firstName;
        private String lastName;
        private String seatNumber;
        private String seatClass;
        private String passportNumber;
    }
}
