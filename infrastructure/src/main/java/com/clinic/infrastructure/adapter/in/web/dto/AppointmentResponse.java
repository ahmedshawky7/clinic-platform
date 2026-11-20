package com.clinic.infrastructure.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AppointmentResponse(
        Long appointmentId,
        Integer queueNumber,
        String status,
        LocalDateTime scheduledAt,
        String visitType,
        BigDecimal fee,
        boolean priority
) {
    public AppointmentResponse(Long appointmentId, Integer queueNumber, String status, LocalDateTime scheduledAt) {
        this(appointmentId, queueNumber, status, scheduledAt, "EXAMINATION", BigDecimal.ZERO, false);
    }
}