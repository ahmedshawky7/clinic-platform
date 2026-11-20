package com.clinic.application.port.out;

import java.time.LocalDate;

public interface UserRegistrationPort {
    boolean existsByUsername(String username);
    Long savePatient(String username, String encodedPassword, String fullName,
                     LocalDate dateOfBirth, String phoneNumber, Long registeredByOrgId);
    void updatePatientToRealAccount(Long patientId, String newUsername, String hashedPassword);
    void updateProfile(Long patientId, String fullName, String phoneNumber);
}