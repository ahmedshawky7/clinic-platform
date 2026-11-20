package com.clinic.application.port.in;

public interface CancelPatientAppointmentUseCase {
    void cancel(Long appointmentId, Long patientId);
}