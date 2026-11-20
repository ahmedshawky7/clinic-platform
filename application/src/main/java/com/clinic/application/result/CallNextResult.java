package com.clinic.application.result;

public record CallNextResult(
        Long completedAppointmentId,
        Long startedAppointmentId,
        Integer currentQueueNumber
) {}