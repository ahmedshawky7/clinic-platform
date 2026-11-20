package com.clinic.infrastructure.adapter.in.web.dto;

import com.clinic.domain.model.enums.VisitType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record BookAppointmentRequest(
        Long patientId,
        @NotNull Long doctorId,
        @NotNull Long clinicId,
        LocalDateTime scheduledAt,
        VisitType visitType
) {}
