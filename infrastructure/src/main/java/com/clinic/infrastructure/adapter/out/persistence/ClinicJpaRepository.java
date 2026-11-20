package com.clinic.infrastructure.adapter.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClinicJpaRepository extends JpaRepository<ClinicEntity, Long> {
    List<ClinicEntity> findByOrganizationId(Long organizationId);
    long countByOrganizationId(Long organizationId);
}