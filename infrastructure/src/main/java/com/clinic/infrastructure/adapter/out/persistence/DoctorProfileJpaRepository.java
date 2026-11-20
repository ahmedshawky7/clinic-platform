package com.clinic.infrastructure.adapter.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DoctorProfileJpaRepository extends JpaRepository<DoctorProfileEntity, Long> {
    Optional<DoctorProfileEntity> findByUserId(Long userId);
    List<DoctorProfileEntity> findByOrganizationId(Long organizationId);
    List<DoctorProfileEntity> findByUserIdIn(List<Long> userIds);
    long countByOrganizationId(Long organizationId);
}
