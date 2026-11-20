package com.clinic.application.port.in;

public interface GeneratePatientAccountUseCase {
    record AccountCredentials(String username, String password) {}
    AccountCredentials generate(Long organizationId, Long patientId);
}