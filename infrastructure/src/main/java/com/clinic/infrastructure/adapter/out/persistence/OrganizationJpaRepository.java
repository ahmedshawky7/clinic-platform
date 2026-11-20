package com.clinic.infrastructure.adapter.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationJpaRepository extends JpaRepository<OrganizationEntity, Long> {
    boolean existsBySlug(String slug);
}