package com.flightreservation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundResponse {

    private Long id;
    private Long bookingId;
    private String bookingReference;
    private BigDecimal refundAmount;
    private String currency;
    private String status;
    private String reason;
    private String refundTransactionId;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
}
