package com.clinic.infrastructure.adapter.out.persistence;

import com.clinic.domain.model.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByUsername(String username);

    @Query("SELECT DISTINCT u FROM UserEntity u WHERE u.role = com.clinic.domain.model.enums.UserRole.PATIENT " +
           "AND (u.organizationId = :orgId OR EXISTS (SELECT 1 FROM AppointmentEntity a WHERE a.patientId = u.id AND a.organizationId = :orgId)) " +
           "AND REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(u.fullName, 'أ', 'ا'), 'إ', 'ا'), 'آ', 'ا'), 'ى', 'ي'), 'ة', 'ه') LIKE %:query%")
    List<UserEntity> searchPatientsInOrg(@Param("orgId") Long orgId, @Param("query") String query);

    @Query("SELECT DISTINCT u FROM UserEntity u WHERE u.role = com.clinic.domain.model.enums.UserRole.PATIENT " +
           "AND (u.organizationId = :orgId OR EXISTS (SELECT 1 FROM AppointmentEntity a WHERE a.patientId = u.id AND a.organizationId = :orgId))")
    List<UserEntity> findPatientsByOrg(@Param("orgId") Long orgId);

    @Query("SELECT COUNT(DISTINCT u.id) FROM UserEntity u WHERE u.role = com.clinic.domain.model.enums.UserRole.PATIENT " +
           "AND (u.organizationId = :orgId OR EXISTS (SELECT 1 FROM AppointmentEntity a WHERE a.patientId = u.id AND a.organizationId = :orgId))")
    long countPatientsByOrg(@Param("orgId") Long orgId);

    @Query("SELECT COUNT(u) > 0 FROM UserEntity u WHERE u.id = :patientId AND u.role = com.clinic.domain.model.enums.UserRole.PATIENT " +
           "AND (u.organizationId = :orgId OR EXISTS (SELECT 1 FROM AppointmentEntity a WHERE a.patientId = u.id AND a.organizationId = :orgId))")
    boolean isPatientInOrg(@Param("orgId") Long orgId, @Param("patientId") Long patientId);

    List<UserEntity> findByOrganizationIdAndRole(Long organizationId, UserRole role);

    List<UserEntity> findByOrganizationIdAndRoleInOrderByIdDesc(Long organizationId, List<UserRole> roles);

    Optional<UserEntity> findFirstByOrganizationIdAndOwnerTrue(Long organizationId);
}