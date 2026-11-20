package com.clinic.application.service;

import com.clinic.application.port.in.GetPatientFullFileUseCase;
import com.clinic.application.port.out.PrescriptionRepositoryPort;
import com.clinic.application.port.out.UserQueryPort;
import com.clinic.domain.model.PatientProfile;

public class GetPatientFullFileService implements GetPatientFullFileUseCase {
    private final UserQueryPort userQueryPort;
    private final PrescriptionRepositoryPort prescriptionRepo;

    public GetPatientFullFileService(UserQueryPort userQueryPort, PrescriptionRepositoryPort prescriptionRepo) {
        this.userQueryPort = userQueryPort;
        this.prescriptionRepo = prescriptionRepo;
    }

    @Override
    public PatientFileDetails getPatientFile(Long organizationId, Long patientId) {
        if (!userQueryPort.isPatientAccessibleByOrg(organizationId, patientId)) {
            throw new IllegalArgumentException("هذا المريض غير مسجل لدى منشأتك");
        }
        PatientProfile profile = userQueryPort.getPatientProfile(patientId);
        var history = prescriptionRepo.findByPatientId(patientId);
        return new PatientFileDetails(profile.id(), profile.fullName(), profile.dateOfBirth(), profile.phoneNumber(), history);
    }
}