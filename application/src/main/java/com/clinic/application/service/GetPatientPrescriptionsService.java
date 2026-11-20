package com.clinic.application.service;

import com.clinic.application.port.in.GetPatientPrescriptionsUseCase;
import com.clinic.application.port.out.PrescriptionRepositoryPort;
import com.clinic.domain.model.Prescription;
import java.util.List;

public class GetPatientPrescriptionsService implements GetPatientPrescriptionsUseCase {
    
    private final PrescriptionRepositoryPort repository;

    public GetPatientPrescriptionsService(PrescriptionRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public List<Prescription> getPrescriptions(Long patientId) {
        return repository.findByPatientId(patientId);
    }
}