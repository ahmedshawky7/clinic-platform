package com.clinic.application.port.in;

public interface RequeuePatientUseCase {
    void requeue(Long appointmentId, Long clinicId, Long doctorId);
}