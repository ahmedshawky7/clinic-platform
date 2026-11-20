package com.clinic.infrastructure.adapter.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SpecialtyJpaRepository extends JpaRepository<SpecialtyEntity, Long> {
    List<SpecialtyEntity> findByOrganizationId(Long organizationId);
}