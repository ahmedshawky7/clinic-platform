package com.clinic.application.port.in;

public interface CompleteConsultationUseCase {
    void complete(Long appointmentId, String diagnosis, String medications);
}