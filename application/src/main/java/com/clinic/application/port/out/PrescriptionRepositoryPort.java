package com.clinic.application.port.out;
import java.util.List;

import com.clinic.domain.model.Prescription;
public interface PrescriptionRepositoryPort {
    Prescription save(Prescription prescription);
    List<Prescription> findByPatientId(Long patientId);
}