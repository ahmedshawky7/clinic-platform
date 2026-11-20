package com.clinic.domain.model;

import java.time.LocalDate;

public record PatientProfile(
    Long id,
    String fullName,
    LocalDate dateOfBirth,
    String phoneNumber,
    boolean hasAccount
) {}