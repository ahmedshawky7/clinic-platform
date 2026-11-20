package com.clinic.application.port.out;

public interface BlockedPatientPort {
    boolean isPatientBlockedInClinic(Long clinicId, Long patientId);
}