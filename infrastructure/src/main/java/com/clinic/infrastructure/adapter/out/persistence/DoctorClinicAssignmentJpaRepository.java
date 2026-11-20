package com.clinic.infrastructure.adapter.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DoctorClinicAssignmentJpaRepository extends JpaRepository<DoctorClinicAssignmentEntity, Long> {
    Optional<DoctorClinicAssignmentEntity> findByClinicIdAndDoctorId(Long clinicId, Long doctorId);
    List<DoctorClinicAssignmentEntity> findByClinicIdAndActiveTrue(Long clinicId);
    List<DoctorClinicAssignmentEntity> findByOrganizationId(Long organizationId);
    List<DoctorClinicAssignmentEntity> findByDoctorIdAndActiveTrue(Long doctorId);
}