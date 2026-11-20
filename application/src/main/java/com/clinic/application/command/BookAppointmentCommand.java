package com.clinic.application.command;

import com.clinic.domain.model.enums.VisitType;

import java.time.LocalDateTime;
import java.util.Objects;

public record BookAppointmentCommand(
        Long patientId,
        Long doctorId,
        Long clinicId,
        LocalDateTime scheduledAt,
        VisitType visitType
) {
    public BookAppointmentCommand {
        Objects.requireNonNull(patientId, "patientId");
        Objects.requireNonNull(doctorId, "doctorId");
        Objects.requireNonNull(clinicId, "clinicId");
        Objects.requireNonNull(scheduledAt, "scheduledAt");
        visitType = visitType != null ? visitType : VisitType.EXAMINATION;
    }

    public BookAppointmentCommand(Long patientId, Long doctorId, Long clinicId, LocalDateTime scheduledAt) {
        this(patientId, doctorId, clinicId, scheduledAt, VisitType.EXAMINATION);
    }
}