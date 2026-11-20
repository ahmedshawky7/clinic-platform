package com.clinic.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BlockedPatientJpaRepository extends JpaRepository<BlockedPatientEntity, Long> {
    boolean existsByClinicIdAndPatientIdAndActiveTrue(Long clinicId, Long patientId);
    boolean existsByClinicIdAndPhoneNumberAndActiveTrue(Long clinicId, String phoneNumber);
    List<BlockedPatientEntity> findByClinicIdAndActiveTrueOrderByCreatedAtDesc(Long clinicId);
    Optional<BlockedPatientEntity> findByIdAndOrganizationId(Long id, Long organizationId);
}