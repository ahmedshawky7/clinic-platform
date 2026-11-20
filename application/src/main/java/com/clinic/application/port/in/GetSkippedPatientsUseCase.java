package com.clinic.application.port.in;

import java.util.List;

public interface GetSkippedPatientsUseCase {
    record SkippedPatientDto(Long appointmentId, int queueNumber, String patientName) {}
    List<SkippedPatientDto> getSkippedPatients(Long clinicId, Long doctorId);
}