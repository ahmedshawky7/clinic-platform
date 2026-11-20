package com.clinic.infrastructure.adapter.out.persistence;

import com.clinic.application.port.out.BlockedPatientPort;
import org.springframework.stereotype.Component;

@Component
public class BlockedPatientAdapter implements BlockedPatientPort {

    private final BlockedPatientJpaRepository blockedRepo;
    private final UserJpaRepository userRepo;

    public BlockedPatientAdapter(BlockedPatientJpaRepository blockedRepo, UserJpaRepository userRepo) {
        this.blockedRepo = blockedRepo;
        this.userRepo = userRepo;
    }

    @Override
    public boolean isPatientBlockedInClinic(Long clinicId, Long patientId) {
        if (blockedRepo.existsByClinicIdAndPatientIdAndActiveTrue(clinicId, patientId)) {
            return true;
        }
        return userRepo.findById(patientId)
                .map(UserEntity::getPhoneNumber)
                .filter(p -> p != null && !p.isBlank())
                .map(p -> blockedRepo.existsByClinicIdAndPhoneNumberAndActiveTrue(clinicId, p.trim()))
                .orElse(false);
    }
}