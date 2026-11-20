package com.clinic.application.port.in;
public interface SkipPatientUseCase {
    void skipCurrent(Long clinicId, Long doctorId);
}