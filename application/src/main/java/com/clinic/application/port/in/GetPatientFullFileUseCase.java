package com.clinic.application.port.in;

import com.clinic.domain.model.Prescription;
import java.time.LocalDate;
import java.util.List;

public interface GetPatientFullFileUseCase {
    record PatientFileDetails(
            Long id,
            String fullName,
            LocalDate dateOfBirth,
            String phoneNumber,
            List<Prescription> history
    ) {}

    PatientFileDetails getPatientFile(Long organizationId, Long patientId);
}