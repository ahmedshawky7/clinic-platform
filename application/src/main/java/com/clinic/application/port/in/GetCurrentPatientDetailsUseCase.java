package com.clinic.application.port.in;

import com.clinic.domain.model.Prescription;
import java.time.LocalDate;
import java.util.List;

public interface GetCurrentPatientDetailsUseCase {
    record PatientDetails(
            String patientName,
            Long appointmentId,
            LocalDate dateOfBirth,
            List<Prescription> history
    ) {}

    PatientDetails getCurrentPatientDetails(Long clinicId, Long doctorId);
}