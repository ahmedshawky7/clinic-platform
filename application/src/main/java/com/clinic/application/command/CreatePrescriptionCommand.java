package com.clinic.application.command;

public record CreatePrescriptionCommand(
        Long appointmentId,
        Long patientId,
        Long doctorId,
        String diagnosis,
        String medications
) {}