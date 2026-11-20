package com.clinic.infrastructure.adapter.out.persistence;

import com.clinic.domain.model.enums.AppointmentStatus;
import com.clinic.domain.model.enums.BookingSource;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentJpaRepository extends JpaRepository<AppointmentEntity, Long> {

    @Query("SELECT a FROM AppointmentEntity a WHERE a.clinicId = :clinicId AND a.doctorId = :doctorId " +
           "AND a.scheduledDate = :date AND a.status = 'IN_PROGRESS' ORDER BY a.queueNumber ASC")
    List<AppointmentEntity> findCurrentInProgress(@Param("clinicId") Long clinicId,
                                                  @Param("doctorId") Long doctorId,
                                                  @Param("date") LocalDate date,
                                                  Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AppointmentEntity a WHERE a.clinicId = :clinicId AND a.doctorId = :doctorId " +
           "AND a.scheduledDate = :date AND a.status = 'WAITING' ORDER BY a.priority DESC, a.queueNumber ASC")
    List<AppointmentEntity> findNextWaitingForUpdate(@Param("clinicId") Long clinicId,
                                                     @Param("doctorId") Long doctorId,
                                                     @Param("date") LocalDate date,
                                                     Pageable pageable);

    @Query("SELECT a FROM AppointmentEntity a WHERE a.clinicId = :clinicId AND a.doctorId = :doctorId " +
           "AND a.scheduledDate = :date AND a.status = 'WAITING' ORDER BY a.priority DESC, a.queueNumber ASC")
    List<AppointmentEntity> findWaitingOrdered(@Param("clinicId") Long clinicId,
                                               @Param("doctorId") Long doctorId,
                                               @Param("date") LocalDate date);

    @Query("SELECT COUNT(a) FROM AppointmentEntity a WHERE a.clinicId = :clinicId AND a.doctorId = :doctorId " +
           "AND a.scheduledDate = :date AND a.status = 'WAITING'")
    int countWaiting(@Param("clinicId") Long clinicId, @Param("doctorId") Long doctorId, @Param("date") LocalDate date);

    @Query("SELECT COALESCE(MAX(a.queueNumber), 0) FROM AppointmentEntity a " +
           "WHERE a.clinicId = :clinicId AND a.doctorId = :doctorId AND a.scheduledDate = :date")
    int maxQueueNumber(@Param("clinicId") Long clinicId, @Param("doctorId") Long doctorId, @Param("date") LocalDate date);

    List<AppointmentEntity> findByClinicIdAndDoctorIdAndScheduledDateAndStatusOrderByQueueNumberAsc(
            Long clinicId, Long doctorId, LocalDate scheduledDate, AppointmentStatus status);

    @Query("SELECT a FROM AppointmentEntity a WHERE a.patientId = :patientId " +
           "AND a.status IN ('WAITING', 'IN_PROGRESS') AND a.scheduledDate = :date ORDER BY a.createdAt DESC")
    List<AppointmentEntity> findActiveByPatientId(@Param("patientId") Long patientId, @Param("date") LocalDate date);

    @Query("SELECT a FROM AppointmentEntity a WHERE a.patientId = :patientId AND a.clinicId = :clinicId " +
           "AND a.scheduledDate = :date AND a.status IN ('WAITING', 'IN_PROGRESS') ORDER BY a.createdAt DESC")
    List<AppointmentEntity> findActiveByPatientInClinic(@Param("patientId") Long patientId,
                                                        @Param("clinicId") Long clinicId,
                                                        @Param("date") LocalDate date);

    @Query("SELECT COUNT(a) > 0 FROM AppointmentEntity a WHERE a.patientId = :patientId " +
           "AND a.clinicId = :clinicId AND a.doctorId = :doctorId AND a.scheduledDate = :date " +
           "AND a.status IN ('WAITING', 'IN_PROGRESS')")
    boolean existsActive(@Param("patientId") Long patientId, @Param("clinicId") Long clinicId,
                         @Param("doctorId") Long doctorId, @Param("date") LocalDate date);

    @Query("SELECT a FROM AppointmentEntity a WHERE a.clinicId = :clinicId AND a.scheduledDate = :date " +
           "AND a.status IN ('WAITING', 'IN_PROGRESS', 'SKIPPED')")
    List<AppointmentEntity> findAllActiveInClinicForDate(@Param("clinicId") Long clinicId, @Param("date") LocalDate date);

    @Query("SELECT a FROM AppointmentEntity a WHERE a.clinicId = :clinicId AND a.doctorId = :doctorId " +
           "AND a.scheduledDate = :date AND a.status IN ('WAITING', 'IN_PROGRESS', 'SKIPPED')")
    List<AppointmentEntity> findAllActiveInClinicAndDoctorForDate(@Param("clinicId") Long clinicId,
                                                                  @Param("doctorId") Long doctorId,
                                                                  @Param("date") LocalDate date);

    List<AppointmentEntity> findByClinicIdAndScheduledDate(Long clinicId, LocalDate date);

    @Query("SELECT a FROM AppointmentEntity a WHERE a.patientId = :patientId AND a.scheduledDate = :date " +
           "ORDER BY a.createdAt DESC")
    List<AppointmentEntity> findLatestByPatientIdAndDate(@Param("patientId") Long patientId, @Param("date") LocalDate date);

    List<AppointmentEntity> findByPatientIdOrderByScheduledDateDescCreatedAtDesc(Long patientId);

    long countByClinicIdAndScheduledDate(Long clinicId, LocalDate date);
    long countByClinicIdAndScheduledDateAndBookingSource(Long clinicId, LocalDate date, BookingSource source);

    @Query("SELECT COUNT(a) FROM AppointmentEntity a WHERE a.organizationId = :orgId " +
           "AND YEAR(a.scheduledDate) = :year AND MONTH(a.scheduledDate) = :month")
    long countByOrgInMonth(@Param("orgId") Long orgId, @Param("year") int year, @Param("month") int month);

    @Query("SELECT a.scheduledDate, COUNT(a) FROM AppointmentEntity a WHERE a.clinicId = :clinicId " +
           "AND a.scheduledDate BETWEEN :start AND :end GROUP BY a.scheduledDate ORDER BY a.scheduledDate")
    List<Object[]> countDailyByClinicBetween(@Param("clinicId") Long clinicId,
                                             @Param("start") LocalDate start,
                                             @Param("end") LocalDate end);
}