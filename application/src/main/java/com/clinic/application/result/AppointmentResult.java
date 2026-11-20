package com.clinic.application.result;

import com.clinic.domain.model.Appointment;
import com.clinic.domain.model.enums.AppointmentStatus;
import com.clinic.domain.model.enums.VisitType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AppointmentResult(
        Long appointmentId,
        Integer queueNumber,
        AppointmentStatus status,
        LocalDateTime scheduledAt,
        VisitType visitType,
        BigDecimal fee,
        boolean priority
) {
    public static AppointmentResult from(Appointment a) {
        return new AppointmentResult(
                a.getId(), a.getQueueNumber(), a.getStatus(), a.getScheduledAt(),
                a.getVisitType(), a.getFee(), a.isPriority());
    }
}