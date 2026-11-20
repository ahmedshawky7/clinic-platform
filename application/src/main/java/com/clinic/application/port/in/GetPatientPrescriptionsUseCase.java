package com.clinic.application.port.in;

import com.clinic.domain.model.Prescription;
import java.util.List;

public interface GetPatientPrescriptionsUseCase {
    List<Prescription> getPrescriptions(Long patientId);
}