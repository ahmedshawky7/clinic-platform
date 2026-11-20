package com.clinic.infrastructure.adapter.out.persistence;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrescriptionJpaRepository extends JpaRepository<PrescriptionEntity, Long>{ 

    List<PrescriptionEntity> findByPatientIdOrderByCreatedAtDesc(Long patientId);
} 